package com.aisafe.business.service.impl;

import com.aisafe.business.document.BizRiskClue;
import com.aisafe.business.dto.CategoryResolveResult;
import com.aisafe.business.dto.ReportMatchPreviewResult;
import com.aisafe.business.dto.RiskReportExcelRow;
import com.aisafe.business.entity.BizRiskReportUploadBatch;
import com.aisafe.business.entity.BizRiskReportUploadDetail;
import com.aisafe.business.entity.BizRiskReportUploadPreview;
import com.aisafe.business.mapper.BizRiskReportUploadBatchMapper;
import com.aisafe.business.mapper.BizRiskReportUploadDetailMapper;
import com.aisafe.business.mapper.BizRiskReportUploadPreviewMapper;
import com.aisafe.business.service.ClueAttachmentService;
import com.aisafe.business.service.RiskClueService;
import com.aisafe.business.service.RiskReportUploadService;
import com.aisafe.business.support.ClueReportFileStorage;
import com.aisafe.business.support.ReportMatchAnalyzer;
import com.aisafe.business.support.RiskClueCategoryValidator;
import com.aisafe.business.support.RiskReportClueBuilder;
import com.aisafe.business.support.RiskReportExcelParser;
import com.aisafe.business.support.RiskReportFileStorage;
import com.aisafe.business.support.RiskReportImportError;
import com.aisafe.business.support.ZipReportExtractor;
import com.aisafe.common.exception.BusinessException;
import com.aisafe.system.entity.SysUser;
import com.aisafe.system.service.ISysUserService;
import com.aisafe.system.service.ITagCategoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RiskReportUploadServiceImpl implements RiskReportUploadService {

    private static final Logger log = LoggerFactory.getLogger(RiskReportUploadServiceImpl.class);
    private static final int MAX_ROWS = 1000;
    private static final int PREVIEW_TTL_HOURS = 2;

    private final BizRiskReportUploadBatchMapper batchMapper;
    private final BizRiskReportUploadDetailMapper detailMapper;
    private final BizRiskReportUploadPreviewMapper previewMapper;
    private final RiskClueService riskClueService;
    private final ClueAttachmentService attachmentService;
    private final ITagCategoryService tagCategoryService;
    private final RiskReportClueBuilder clueBuilder;
    private final RiskReportFileStorage fileStorage;
    private final ClueReportFileStorage clueReportFileStorage;
    private final ZipReportExtractor zipReportExtractor;
    private final ReportMatchAnalyzer reportMatchAnalyzer;
    private final ISysUserService userService;
    private final RiskReportUploadAsyncProcessor asyncProcessor;
    private final ObjectMapper objectMapper;

    public RiskReportUploadServiceImpl(BizRiskReportUploadBatchMapper batchMapper,
                                       BizRiskReportUploadDetailMapper detailMapper,
                                       BizRiskReportUploadPreviewMapper previewMapper,
                                       RiskClueService riskClueService,
                                       ClueAttachmentService attachmentService,
                                       ITagCategoryService tagCategoryService,
                                       RiskReportClueBuilder clueBuilder,
                                       RiskReportFileStorage fileStorage,
                                       ClueReportFileStorage clueReportFileStorage,
                                       ZipReportExtractor zipReportExtractor,
                                       ReportMatchAnalyzer reportMatchAnalyzer,
                                       ISysUserService userService,
                                       @Lazy RiskReportUploadAsyncProcessor asyncProcessor,
                                       ObjectMapper objectMapper) {
        this.batchMapper = batchMapper;
        this.detailMapper = detailMapper;
        this.previewMapper = previewMapper;
        this.riskClueService = riskClueService;
        this.attachmentService = attachmentService;
        this.tagCategoryService = tagCategoryService;
        this.clueBuilder = clueBuilder;
        this.fileStorage = fileStorage;
        this.clueReportFileStorage = clueReportFileStorage;
        this.zipReportExtractor = zipReportExtractor;
        this.reportMatchAnalyzer = reportMatchAnalyzer;
        this.userService = userService;
        this.asyncProcessor = asyncProcessor;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> previewUpload(MultipartFile excel, MultipartFile zip,
                                             SysUser user, String submitOrgName) {
        validateExcelFile(excel);
        LocalDateTime now = LocalDateTime.now();
        String previewToken = UUID.randomUUID().toString().replace("-", "");
        Path sessionDir = clueReportFileStorage.previewSessionDir(previewToken, now);
        Path excelPath;
        Path zipPath = null;
        Path extractedDir = null;
        try {
            excelPath = sessionDir.resolve("upload.xlsx");
            excel.transferTo(excelPath.toFile());
            if (zip != null && !zip.isEmpty()) {
                clueReportFileStorage.validateZipMultipart(zip);
                zipPath = sessionDir.resolve("reports.zip");
                zip.transferTo(zipPath.toFile());
                extractedDir = sessionDir.resolve("extracted");
                zipReportExtractor.extract(zipPath, extractedDir);
            }
        } catch (IOException e) {
            throw new BusinessException("保存预览文件失败: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(e.getMessage());
        }

        List<RiskReportExcelRow> rows;
        try {
            rows = RiskReportExcelParser.parse(excelPath);
        } catch (Exception e) {
            throw new BusinessException("Excel 解析失败: " + e.getMessage());
        }
        if (rows.isEmpty()) {
            throw new BusinessException("Excel 中没有有效数据行");
        }
        if (rows.size() > MAX_ROWS) {
            throw new BusinessException("单次上传最多支持 " + MAX_ROWS + " 条记录");
        }

        ReportMatchPreviewResult match = reportMatchAnalyzer.analyze(rows, extractedDir);
        match.setInvalidRows(validateExcelRows(rows));
        match.setPreviewToken(previewToken);

        BizRiskReportUploadPreview preview = new BizRiskReportUploadPreview();
        preview.setPreviewToken(previewToken);
        preview.setExcelPath(excelPath.toString());
        preview.setExcelFileName(originalUploadName(excel, "upload.xlsx"));
        preview.setZipPath(zipPath != null ? zipPath.toString() : null);
        if (zip != null && !zip.isEmpty()) {
            preview.setZipFileName(originalUploadName(zip, "reports.zip"));
        }
        preview.setExtractedDir(extractedDir != null ? extractedDir.toString() : null);
        preview.setSubmitUserId(user.getId());
        preview.setSubmitOrgName(submitOrgName);
        preview.setExpireTime(now.plusHours(PREVIEW_TTL_HOURS));
        preview.setCreateTime(now);
        try {
            preview.setMatchResultJson(objectMapper.writeValueAsString(match.toMap()));
        } catch (JsonProcessingException e) {
            throw new BusinessException("保存匹配结果失败");
        }
        previewMapper.insert(preview);
        return match.toMap();
    }

    @Override
    public Map<String, Object> confirmUpload(String previewToken, SysUser user, String submitOrgName) {
        if (!StringUtils.hasText(previewToken)) {
            throw new BusinessException("previewToken 不能为空");
        }
        BizRiskReportUploadPreview preview = previewMapper.selectOne(
                new LambdaQueryWrapper<BizRiskReportUploadPreview>()
                        .eq(BizRiskReportUploadPreview::getPreviewToken, previewToken.trim()));
        if (preview == null) {
            throw new BusinessException("预览已失效，请重新校验");
        }
        if (preview.getExpireTime() != null && preview.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("预览已过期，请重新校验");
        }
        if (preview.getSubmitUserId() != null && !preview.getSubmitUserId().equals(user.getId())) {
            throw new BusinessException("无权确认该预览会话");
        }

        List<RiskReportExcelRow> previewRows;
        try {
            previewRows = RiskReportExcelParser.parse(Path.of(preview.getExcelPath()));
        } catch (Exception e) {
            throw new BusinessException("Excel 解析失败: " + e.getMessage());
        }
        List<ReportMatchPreviewResult.InvalidRow> invalidRows = validateExcelRows(previewRows);
        if (!invalidRows.isEmpty()) {
            throw new BusinessException("Excel 存在 " + invalidRows.size() + " 行数据校验失败，请修正后重新预览");
        }

        LocalDateTime now = LocalDateTime.now();
        String batchNo = UUID.randomUUID().toString().replace("-", "");
        ArchivedBatchPaths archived;
        try {
            archived = archivePreviewFiles(preview, submitOrgName, batchNo, now);
        } catch (IOException e) {
            throw new BusinessException("归档上传文件失败: " + e.getMessage());
        }

        BizRiskReportUploadBatch batch = new BizRiskReportUploadBatch();
        batch.setBatchNo(batchNo);
        batch.setFileName(resolveStoredFileName(preview.getExcelFileName(), preview.getExcelPath(), "upload.xlsx"));
        batch.setFilePath(archived.excelPath().toString());
        batch.setFileSize(fileSizeAtPath(archived.excelPath().toString()));
        batch.setStatus("processing");
        batch.setTotalCount(0);
        batch.setProcessedCount(0);
        batch.setSuccessCount(0);
        batch.setFailCount(0);
        batch.setSubmitUserId(user.getId());
        batch.setSubmitUserName(displayName(user));
        batch.setSubmitOrgName(submitOrgName);
        batch.setSubmitTime(now);
        if (archived.zipPath() != null) {
            batch.setZipFileName(resolveStoredFileName(preview.getZipFileName(), preview.getZipPath(), "reports.zip"));
            batch.setZipFilePath(archived.zipPath().toString());
        }
        if (archived.extractedDir() != null) {
            batch.setExtractedDir(archived.extractedDir().toString());
        }
        applyMatchSummaryToBatch(batch, preview.getMatchResultJson());
        batchMapper.insert(batch);

        previewMapper.deleteById(preview.getId());
        asyncProcessor.process(batch.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("batchId", batch.getId());
        result.put("batchNo", batch.getBatchNo());
        result.put("status", batch.getStatus());
        result.put("fileName", batch.getFileName());
        return result;
    }

    @Override
    public Map<String, Object> startUpload(MultipartFile file, MultipartFile zip, SysUser user, String submitOrgName) {
        validateExcelFile(file);

        String originalName = file.getOriginalFilename();
        LocalDateTime now = LocalDateTime.now();
        String batchNo = UUID.randomUUID().toString().replace("-", "");
        BizRiskReportUploadBatch batch = new BizRiskReportUploadBatch();
        batch.setBatchNo(batchNo);
        batch.setFileName(originalName);
        batch.setFileSize(file.getSize());
        batch.setStatus("processing");
        batch.setTotalCount(0);
        batch.setProcessedCount(0);
        batch.setSuccessCount(0);
        batch.setFailCount(0);
        batch.setSubmitUserId(user.getId());
        batch.setSubmitUserName(displayName(user));
        batch.setSubmitOrgName(submitOrgName);
        batch.setSubmitTime(now);
        batchMapper.insert(batch);

        try {
            Path saved = fileStorage.saveUploadFile(file, submitOrgName, batchNo, now);
            batch.setFilePath(saved.toString());
            if (zip != null && !zip.isEmpty()) {
                clueReportFileStorage.validateZipMultipart(zip);
                Path batchRoot = clueReportFileStorage.batchRootDir(submitOrgName, batchNo, now);
                Path zipPath = batchRoot.resolve("reports.zip");
                zip.transferTo(zipPath.toFile());
                batch.setZipFileName(originalUploadName(zip, "reports.zip"));
                batch.setZipFilePath(zipPath.toString());
                Path extractedDir = clueReportFileStorage.batchExtractDir(submitOrgName, batchNo, now);
                zipReportExtractor.extract(zipPath, extractedDir);
                batch.setExtractedDir(extractedDir.toString());
            }
            batchMapper.updateById(batch);
        } catch (Exception e) {
            batch.setStatus("fail");
            batch.setErrorSummary("文件保存失败: " + e.getMessage());
            batch.setFinishTime(LocalDateTime.now());
            batchMapper.updateById(batch);
            throw new BusinessException("文件保存失败: " + e.getMessage());
        }

        asyncProcessor.process(batch.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("batchId", batch.getId());
        result.put("batchNo", batch.getBatchNo());
        result.put("status", batch.getStatus());
        result.put("fileName", batch.getFileName());
        return result;
    }

    @Override
    public void processBatchAsync(Long batchId) {
        processBatch(batchId);
    }

    public void processBatch(Long batchId) {
        BizRiskReportUploadBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            return;
        }
        try {
            if (!StringUtils.hasText(batch.getFilePath())) {
                failBatch(batch, "文件路径为空");
                return;
            }

            List<RiskReportExcelRow> rows = RiskReportExcelParser.parse(Path.of(batch.getFilePath()));
            if (rows.isEmpty()) {
                failBatch(batch, "Excel 中没有有效数据行");
                return;
            }
            if (rows.size() > MAX_ROWS) {
                failBatch(batch, "单次上传最多支持 " + MAX_ROWS + " 条记录");
                return;
            }

            batch.setTotalCount(rows.size());
            batch.setProcessedCount(0);
            batch.setSuccessCount(0);
            batch.setFailCount(0);
            batchMapper.updateById(batch);

            RiskClueCategoryValidator categoryValidator =
                    new RiskClueCategoryValidator(tagCategoryService.getTreeByModule("risk_clue"));

            Map<Integer, List<Path>> serialFiles = StringUtils.hasText(batch.getExtractedDir())
                    ? attachmentService.indexExtractedReports(Path.of(batch.getExtractedDir()))
                    : Map.of();

            SysUser loginUser = userService.getById(batch.getSubmitUserId());
            if (loginUser == null) {
                loginUser = new SysUser();
                loginUser.setId(batch.getSubmitUserId());
                loginUser.setNickname(batch.getSubmitUserName());
                loginUser.setUsername(batch.getSubmitUserName());
            }

            int success = 0;
            int fail = 0;
            for (RiskReportExcelRow row : rows) {
                try {
                    processRow(batch, row, categoryValidator, loginUser, serialFiles);
                    success++;
                } catch (RowImportException e) {
                    fail++;
                    saveFailDetail(batch.getId(), row, e.error);
                } catch (Exception e) {
                    fail++;
                    saveFailDetail(batch.getId(), row, RiskReportImportError.of("IMPORT_ERROR",
                            "第 " + row.getRowNum() + " 行：导入异常 " + e.getMessage()));
                }
                batch.setProcessedCount(success + fail);
                batch.setSuccessCount(success);
                batch.setFailCount(fail);
                batchMapper.updateById(batch);
            }

            batch.setFinishTime(LocalDateTime.now());
            if (fail == 0) {
                batch.setStatus("success");
            } else if (success == 0) {
                batch.setStatus("fail");
            } else {
                batch.setStatus("partial_fail");
            }
            batchMapper.updateById(batch);
        } catch (Exception e) {
            log.error("批量导入处理失败 batchId={}", batchId, e);
            failBatch(batch, e.getMessage());
        }
    }

    private void processRow(BizRiskReportUploadBatch batch,
                            RiskReportExcelRow row,
                            RiskClueCategoryValidator categoryValidator,
                            SysUser loginUser,
                            Map<Integer, List<Path>> serialFiles) {
        RiskReportImportError error = clueBuilder.validateRowFields(row, categoryValidator);
        if (error != null) {
            throw new RowImportException(error);
        }
        CategoryResolveResult category = categoryValidator.resolve(row);
        if (!category.isSuccess()) {
            throw new RowImportException(category.getError());
        }
        BizRiskClue clue = clueBuilder.buildPendingClue(
                row, loginUser, batch.getSubmitTime(), category.getLevel1(), category.getLevel2());
        if (StringUtils.hasText(batch.getSubmitOrgName())) {
            clue.setSubmitOrgName(batch.getSubmitOrgName());
        }
        String clueId = riskClueService.save(clue);

        String attachmentStatus = "none";
        String attachmentNames = null;
        if (row.getNumber() != null && serialFiles != null && !serialFiles.isEmpty()) {
            List<Path> files = serialFiles.get(row.getNumber());
            if (files != null && !files.isEmpty()) {
                attachmentService.bindBatchFiles(
                        clueId, batch.getId(), row.getNumber(), files, loginUser, batch.getSubmitOrgName());
                attachmentStatus = "matched";
                attachmentNames = files.stream()
                        .map(path -> path.getFileName().toString())
                        .reduce((a, b) -> a + ", " + b)
                        .orElse(null);
            } else if (StringUtils.hasText(batch.getExtractedDir())) {
                attachmentStatus = "missing";
            }
        }
        saveSuccessDetail(batch.getId(), row, clueId, attachmentStatus, attachmentNames);
    }

    private void saveSuccessDetail(Long batchId, RiskReportExcelRow row, String clueId,
                                   String attachmentStatus, String attachmentNames) {
        BizRiskReportUploadDetail detail = new BizRiskReportUploadDetail();
        detail.setBatchId(batchId);
        detail.setSerialNo(row.getNumber());
        detail.setEventName(trimEventName(row.getEventName()));
        detail.setStatus("success");
        detail.setClueId(clueId);
        detail.setAttachmentStatus(attachmentStatus);
        detail.setAttachmentNames(attachmentNames);
        detail.setCreateTime(LocalDateTime.now());
        detailMapper.insert(detail);
    }

    private void saveFailDetail(Long batchId, RiskReportExcelRow row, RiskReportImportError error) {
        BizRiskReportUploadDetail detail = new BizRiskReportUploadDetail();
        detail.setBatchId(batchId);
        detail.setSerialNo(row.getNumber());
        detail.setEventName(trimEventName(row.getEventName()));
        detail.setStatus("fail");
        detail.setErrorMessage(error.getErrorMessage());
        detail.setCreateTime(LocalDateTime.now());
        detailMapper.insert(detail);
    }

    private static String trimEventName(String eventName) {
        if (!StringUtils.hasText(eventName)) {
            return null;
        }
        String trimmed = eventName.trim();
        return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
    }

    private void failBatch(BizRiskReportUploadBatch batch, String message) {
        batch.setStatus("fail");
        batch.setErrorSummary(message);
        batch.setFinishTime(LocalDateTime.now());
        batchMapper.updateById(batch);
    }

    @Override
    public Map<String, Object> getProgress(Long batchId, SysUser user, String submitOrgName, boolean superAdmin) {
        BizRiskReportUploadBatch batch = requireBatch(batchId, submitOrgName, superAdmin);
        Map<String, Object> map = new HashMap<>();
        map.put("batchId", batch.getId());
        map.put("status", batch.getStatus());
        map.put("totalCount", batch.getTotalCount());
        map.put("processedCount", batch.getProcessedCount());
        map.put("successCount", batch.getSuccessCount());
        map.put("failCount", batch.getFailCount());
        int total = batch.getTotalCount() != null && batch.getTotalCount() > 0 ? batch.getTotalCount() : 1;
        int processed = batch.getProcessedCount() != null ? batch.getProcessedCount() : 0;
        map.put("percent", Math.min(100, processed * 100 / total));
        map.put("errorSummary", batch.getErrorSummary());
        return map;
    }

    @Override
    public Map<String, Object> listBatches(int page, int size, String status, String keyword,
                                           String submitUserName, String submitTimeStart, String submitTimeEnd,
                                           SysUser user, String submitOrgName, boolean superAdmin) {
        Page<BizRiskReportUploadBatch> pageQuery = new Page<>(page, size);
        LambdaQueryWrapper<BizRiskReportUploadBatch> wrapper = new LambdaQueryWrapper<>();
        if (!superAdmin && !StringUtils.hasText(submitOrgName)) {
            throw new BusinessException("当前用户未分配部门，无法查看上传批次");
        }
        if (!superAdmin && StringUtils.hasText(submitOrgName)) {
            wrapper.eq(BizRiskReportUploadBatch::getSubmitOrgName, submitOrgName);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(BizRiskReportUploadBatch::getStatus, status.trim());
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(BizRiskReportUploadBatch::getFileName, keyword.trim());
        }
        if (StringUtils.hasText(submitUserName)) {
            wrapper.like(BizRiskReportUploadBatch::getSubmitUserName, submitUserName.trim());
        }
        LocalDateTime rangeStart = parseDateStart(submitTimeStart);
        LocalDateTime rangeEnd = parseDateEnd(submitTimeEnd);
        if (rangeStart != null) {
            wrapper.ge(BizRiskReportUploadBatch::getSubmitTime, rangeStart);
        }
        if (rangeEnd != null) {
            wrapper.le(BizRiskReportUploadBatch::getSubmitTime, rangeEnd);
        }
        wrapper.orderByDesc(BizRiskReportUploadBatch::getSubmitTime);
        Page<BizRiskReportUploadBatch> result = batchMapper.selectPage(pageQuery, wrapper);
        Map<String, Object> map = new HashMap<>();
        map.put("total", result.getTotal());
        map.put("rows", result.getRecords());
        return map;
    }

    @Override
    public Map<String, Object> getBatch(Long batchId, SysUser user, String submitOrgName, boolean superAdmin) {
        BizRiskReportUploadBatch batch = requireBatch(batchId, submitOrgName, superAdmin);
        Map<String, Object> map = new HashMap<>();
        map.put("batch", batch);
        return map;
    }

    @Override
    public Map<String, Object> listDetails(Long batchId, int page, int size, String status,
                                           SysUser user, String submitOrgName, boolean superAdmin) {
        requireBatch(batchId, submitOrgName, superAdmin);
        Page<BizRiskReportUploadDetail> pageQuery = new Page<>(page, size);
        LambdaQueryWrapper<BizRiskReportUploadDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizRiskReportUploadDetail::getBatchId, batchId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(BizRiskReportUploadDetail::getStatus, status);
        }
        wrapper.orderByAsc(BizRiskReportUploadDetail::getSerialNo)
                .orderByAsc(BizRiskReportUploadDetail::getId);
        Page<BizRiskReportUploadDetail> result = detailMapper.selectPage(pageQuery, wrapper);
        Map<String, Object> map = new HashMap<>();
        map.put("total", result.getTotal());
        map.put("rows", result.getRecords());
        return map;
    }

    private BizRiskReportUploadBatch requireBatch(Long batchId, String submitOrgName, boolean superAdmin) {
        BizRiskReportUploadBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException("上传批次不存在");
        }
        if (!superAdmin) {
            if (!StringUtils.hasText(submitOrgName)) {
                throw new BusinessException("当前用户未分配部门，无法查看上传批次");
            }
            if (StringUtils.hasText(batch.getSubmitOrgName())
                    && !submitOrgName.equals(batch.getSubmitOrgName())) {
                throw new BusinessException("无权查看该上传批次");
            }
        }
        return batch;
    }

    private static String displayName(SysUser user) {
        if (user == null) {
            return "";
        }
        return StringUtils.hasText(user.getNickname()) ? user.getNickname().trim() : user.getUsername();
    }

    private static LocalDateTime parseDateStart(String text) {
        LocalDate date = parseDate(text);
        return date != null ? date.atStartOfDay() : null;
    }

    private static LocalDateTime parseDateEnd(String text) {
        LocalDate date = parseDate(text);
        return date != null ? date.atTime(23, 59, 59) : null;
    }

    private static LocalDate parseDate(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String value = text.trim();
        try {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ignored) {
            // continue
        }
        try {
            return LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static String originalUploadName(MultipartFile file, String fallback) {
        if (file == null || !StringUtils.hasText(file.getOriginalFilename())) {
            return fallback;
        }
        return ClueReportFileStorage.sanitizeOriginalName(file.getOriginalFilename());
    }

    private static String resolveStoredFileName(String storedName, String filePath, String fallback) {
        if (StringUtils.hasText(storedName)) {
            return storedName.trim();
        }
        if (StringUtils.hasText(filePath)) {
            return Path.of(filePath).getFileName().toString();
        }
        return fallback;
    }

    private static Long fileSizeAtPath(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            return null;
        }
        try {
            return Files.size(Path.of(filePath));
        } catch (IOException e) {
            return null;
        }
    }

    private void validateExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择 Excel 文件");
        }
        String originalName = file.getOriginalFilename();
        if (!StringUtils.hasText(originalName) || !originalName.toLowerCase().endsWith(".xlsx")) {
            throw new BusinessException("仅支持 .xlsx 格式文件");
        }
    }

    private void applyMatchSummaryToBatch(BizRiskReportUploadBatch batch, String matchJson) {
        if (!StringUtils.hasText(matchJson)) {
            batch.setReportMatchedCount(0);
            batch.setReportMissingCount(0);
            batch.setReportOrphanCount(0);
            return;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(matchJson, Map.class);
            batch.setReportMatchedCount(intValue(map.get("matchedSerialCount")));
            @SuppressWarnings("unchecked")
            List<Object> missing = (List<Object>) map.get("missingReportSerials");
            batch.setReportMissingCount(missing != null ? missing.size() : 0);
            @SuppressWarnings("unchecked")
            List<Object> unmatched = (List<Object>) map.get("unmatchedFiles");
            batch.setReportOrphanCount(unmatched != null ? unmatched.size() : 0);
            batch.setReportMatchSummary(matchJson);
        } catch (JsonProcessingException e) {
            batch.setReportMatchSummary(matchJson);
        }
    }

    private static int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    private List<ReportMatchPreviewResult.InvalidRow> validateExcelRows(List<RiskReportExcelRow> rows) {
        RiskClueCategoryValidator categoryValidator =
                new RiskClueCategoryValidator(tagCategoryService.getTreeByModule("risk_clue"));
        List<ReportMatchPreviewResult.InvalidRow> invalidRows = new ArrayList<>();
        for (RiskReportExcelRow row : rows) {
            RiskReportImportError error = clueBuilder.validateRowFields(row, categoryValidator);
            if (error != null) {
                invalidRows.add(new ReportMatchPreviewResult.InvalidRow(
                        row.getRowNum(), row.getNumber(), error.getErrorMessage()));
                continue;
            }
            CategoryResolveResult category = categoryValidator.resolve(row);
            if (!category.isSuccess()) {
                invalidRows.add(new ReportMatchPreviewResult.InvalidRow(
                        row.getRowNum(), row.getNumber(), category.getError().getErrorMessage()));
            }
        }
        return invalidRows;
    }

    private ArchivedBatchPaths archivePreviewFiles(BizRiskReportUploadPreview preview,
                                                   String submitOrgName,
                                                   String batchNo,
                                                   LocalDateTime now) throws IOException {
        Path batchRoot = clueReportFileStorage.batchRootDir(submitOrgName, batchNo, now);
        String excelFileName = resolveStoredFileName(
                preview.getExcelFileName(), preview.getExcelPath(), "upload.xlsx");
        Path excelDest = batchRoot.resolve(excelFileName);
        Files.copy(Path.of(preview.getExcelPath()), excelDest, StandardCopyOption.REPLACE_EXISTING);

        Path zipDest = null;
        Path extractedDest = null;
        if (StringUtils.hasText(preview.getZipPath())) {
            String zipFileName = resolveStoredFileName(
                    preview.getZipFileName(), preview.getZipPath(), "reports.zip");
            zipDest = batchRoot.resolve(zipFileName);
            Files.copy(Path.of(preview.getZipPath()), zipDest, StandardCopyOption.REPLACE_EXISTING);
        }
        if (StringUtils.hasText(preview.getExtractedDir())) {
            extractedDest = clueReportFileStorage.batchExtractDir(submitOrgName, batchNo, now);
            copyDirectory(Path.of(preview.getExtractedDir()), extractedDest);
        }
        return new ArchivedBatchPaths(excelDest, zipDest, extractedDest);
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source).forEach(sourcePath -> {
            try {
                Path targetPath = target.resolve(source.relativize(sourcePath));
                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        });
    }

    private record ArchivedBatchPaths(Path excelPath, Path zipPath, Path extractedDir) {
    }

    private static class RowImportException extends RuntimeException {
        private final RiskReportImportError error;

        RowImportException(RiskReportImportError error) {
            super(error.getErrorMessage());
            this.error = error;
        }
    }
}
