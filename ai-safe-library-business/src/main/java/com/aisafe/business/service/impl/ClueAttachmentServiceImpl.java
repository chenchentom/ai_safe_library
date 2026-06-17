package com.aisafe.business.service.impl;

import com.aisafe.business.document.BizRiskClue;
import com.aisafe.business.dto.ReportAttachmentSnapshot;
import com.aisafe.business.entity.BizRiskClueAttachment;
import com.aisafe.business.mapper.BizRiskClueAttachmentMapper;
import com.aisafe.business.service.ClueAttachmentService;
import com.aisafe.business.service.RiskClueService;
import com.aisafe.business.support.ClueReportFileStorage;
import com.aisafe.business.support.ReportSerialFilenameMatcher;
import com.aisafe.common.exception.BusinessException;
import com.aisafe.system.entity.SysUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ClueAttachmentServiceImpl implements ClueAttachmentService {

    private static final DateTimeFormatter ES_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BizRiskClueAttachmentMapper attachmentMapper;
    private final RiskClueService riskClueService;
    private final ClueReportFileStorage fileStorage;
    private final ReportSerialFilenameMatcher filenameMatcher;
    private final ElasticsearchOperations elasticsearchOperations;

    public ClueAttachmentServiceImpl(BizRiskClueAttachmentMapper attachmentMapper,
                                     RiskClueService riskClueService,
                                     ClueReportFileStorage fileStorage,
                                     ReportSerialFilenameMatcher filenameMatcher,
                                     ElasticsearchOperations elasticsearchOperations) {
        this.attachmentMapper = attachmentMapper;
        this.riskClueService = riskClueService;
        this.fileStorage = fileStorage;
        this.filenameMatcher = filenameMatcher;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @Override
    public List<BizRiskClueAttachment> listByClueId(String clueId) {
        if (!StringUtils.hasText(clueId)) {
            return List.of();
        }
        LambdaQueryWrapper<BizRiskClueAttachment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizRiskClueAttachment::getClueId, clueId.trim())
                .orderByAsc(BizRiskClueAttachment::getSortOrder)
                .orderByAsc(BizRiskClueAttachment::getId);
        return attachmentMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ReportAttachmentSnapshot> upload(String clueId, MultipartFile[] files, SysUser user, String deptName) {
        requireClue(clueId);
        if (files == null || files.length == 0) {
            throw new BusinessException("请选择报告文件");
        }
        int existing = listByClueId(clueId).size();
        if (existing + files.length > ClueReportFileStorage.MAX_FILES_PER_CLUE) {
            throw new BusinessException("每条线索最多 " + ClueReportFileStorage.MAX_FILES_PER_CLUE + " 个报告");
        }

        LocalDateTime now = LocalDateTime.now();
        Path clueDir = fileStorage.clueAttachmentDir(deptName, clueId.trim(), now);
        List<BizRiskClueAttachment> saved = new ArrayList<>();
        int sort = existing;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            try {
                Path storedPath = fileStorage.saveMultipart(file, clueDir);
                BizRiskClueAttachment row = buildAttachmentRow(
                        clueId.trim(), null, null, file.getOriginalFilename(), storedPath, user, now, sort++);
                attachmentMapper.insert(row);
                saved.add(row);
            } catch (IOException e) {
                throw new BusinessException("保存文件失败: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                throw new BusinessException(e.getMessage());
            }
        }
        if (saved.isEmpty()) {
            throw new BusinessException("没有有效的报告文件");
        }
        syncEsSnapshot(clueId.trim());
        return toSnapshots(saved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindBatchFiles(String clueId, Long batchId, Integer serialNo, List<Path> sourceFiles,
                               SysUser user, String deptName) {
        if (sourceFiles == null || sourceFiles.isEmpty()) {
            return;
        }
        requireClue(clueId);
        int existing = listByClueId(clueId).size();
        if (existing + sourceFiles.size() > ClueReportFileStorage.MAX_FILES_PER_CLUE) {
            throw new BusinessException("线索 " + clueId + " 报告数量超过上限");
        }
        LocalDateTime now = LocalDateTime.now();
        Path clueDir = fileStorage.clueAttachmentDir(deptName, clueId.trim(), now);
        int sort = existing;
        for (Path source : sourceFiles) {
            try {
                String originalName = source.getFileName().toString();
                Path storedPath = fileStorage.copyToClueDir(source, clueDir, originalName);
                BizRiskClueAttachment row = buildAttachmentRow(
                        clueId.trim(), batchId, serialNo, originalName, storedPath, user, now, sort++);
                attachmentMapper.insert(row);
            } catch (IOException e) {
                throw new BusinessException("绑定报告失败: " + e.getMessage());
            }
        }
        syncEsSnapshot(clueId.trim());
    }

    @Override
    public BizRiskClueAttachment requireReadable(Long attachmentId) {
        if (attachmentId == null) {
            throw new BusinessException("附件不存在");
        }
        BizRiskClueAttachment attachment = attachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            throw new BusinessException("附件不存在");
        }
        requireClue(attachment.getClueId());
        Path path = Path.of(attachment.getStoragePath());
        if (!Files.isRegularFile(path)) {
            throw new BusinessException("报告文件不存在或已被删除");
        }
        return attachment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long attachmentId, SysUser user, String reportUnit) {
        BizRiskClueAttachment attachment = requireReadable(attachmentId);
        BizRiskClue clue = riskClueService.getById(attachment.getClueId());
        if (clue != null && clue.getAuditStatus() != null && clue.getAuditStatus() == 10) {
            if (StringUtils.hasText(reportUnit) && StringUtils.hasText(clue.getSubmitOrgName())
                    && !reportUnit.equals(clue.getSubmitOrgName())) {
                throw new BusinessException("无权删除该报告");
            }
        }
        attachmentMapper.deleteById(attachmentId);
        syncEsSnapshot(attachment.getClueId());
        try {
            Files.deleteIfExists(Path.of(attachment.getStoragePath()));
        } catch (IOException ignored) {
            // 数据库记录已删，磁盘残留可后续清理
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAllByClueId(String clueId) {
        if (!StringUtils.hasText(clueId)) {
            return;
        }
        List<BizRiskClueAttachment> attachments = listByClueId(clueId.trim());
        List<String> storagePaths = new ArrayList<>();
        for (BizRiskClueAttachment attachment : attachments) {
            storagePaths.add(attachment.getStoragePath());
            attachmentMapper.deleteById(attachment.getId());
        }
        syncEsSnapshot(clueId.trim());
        for (String storagePath : storagePaths) {
            try {
                Files.deleteIfExists(Path.of(storagePath));
            } catch (IOException ignored) {
                // ignore
            }
        }
    }

    @Override
    public void syncEsSnapshot(String clueId) {
        if (!StringUtils.hasText(clueId)) {
            return;
        }
        List<BizRiskClueAttachment> attachments = listByClueId(clueId.trim());
        List<Map<String, Object>> snapshots = attachments.stream().map(row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", String.valueOf(row.getId()));
            item.put("file_name", row.getOriginalName());
            item.put("content_type", row.getContentType());
            item.put("size", row.getFileSize());
            return item;
        }).collect(Collectors.toList());

        Document doc = Document.create();
        doc.put("has_report", attachments.isEmpty() ? 0 : 1);
        doc.put("report_attachments", snapshots.isEmpty() ? null : snapshots);
        doc.put("update_time", LocalDateTime.now().format(ES_DATE_TIME));

        UpdateQuery updateQuery = UpdateQuery.builder(clueId.trim())
                .withDocument(doc)
                .build();
        try {
            elasticsearchOperations.update(updateQuery,
                    elasticsearchOperations.getIndexCoordinatesFor(BizRiskClue.class));
        } catch (Exception e) {
            throw new BusinessException("同步附件快照失败: " + e.getMessage());
        }
    }

    @Override
    public Map<Integer, List<Path>> indexExtractedReports(Path extractedDir) {
        Map<Integer, List<Path>> map = new HashMap<>();
        if (extractedDir == null || !Files.isDirectory(extractedDir)) {
            return map;
        }
        try (var stream = Files.list(extractedDir)) {
            stream.filter(Files::isRegularFile).forEach(path -> {
                String name = path.getFileName().toString();
                filenameMatcher.parseSerial(name).ifPresent(serial ->
                        map.computeIfAbsent(serial, k -> new ArrayList<>()).add(path));
            });
        } catch (IOException e) {
            throw new BusinessException("读取解压目录失败: " + e.getMessage());
        }
        return map;
    }

    private BizRiskClue requireClue(String clueId) {
        BizRiskClue clue = riskClueService.getById(clueId);
        if (clue == null) {
            throw new BusinessException("线索不存在");
        }
        return clue;
    }

    private BizRiskClueAttachment buildAttachmentRow(String clueId, Long batchId, Integer serialNo,
                                                     String originalName, Path storedPath,
                                                     SysUser user, LocalDateTime now, int sortOrder) {
        BizRiskClueAttachment row = new BizRiskClueAttachment();
        row.setClueId(clueId);
        row.setBatchId(batchId);
        row.setSerialNo(serialNo);
        row.setOriginalName(ClueReportFileStorage.sanitizeOriginalName(originalName));
        row.setStoredName(storedPath.getFileName().toString());
        row.setStoragePath(storedPath.toAbsolutePath().toString());
        row.setContentType(ClueReportFileStorage.contentTypeOf(row.getOriginalName()));
        try {
            row.setFileSize(Files.size(storedPath));
        } catch (IOException e) {
            row.setFileSize(0L);
        }
        row.setSortOrder(sortOrder);
        if (user != null) {
            row.setUploadUserId(user.getId());
        }
        row.setUploadTime(now);
        return row;
    }

    private List<ReportAttachmentSnapshot> toSnapshots(List<BizRiskClueAttachment> rows) {
        List<ReportAttachmentSnapshot> list = new ArrayList<>();
        for (BizRiskClueAttachment row : rows) {
            list.add(new ReportAttachmentSnapshot(
                    String.valueOf(row.getId()),
                    row.getOriginalName(),
                    row.getContentType(),
                    row.getFileSize()));
        }
        return list;
    }
}
