package com.aisafe.business.service.impl;

import com.aisafe.business.document.BizRiskClue;
import com.aisafe.business.dto.RiskReportExcelRow;
import com.aisafe.business.entity.BizRiskReportUploadBatch;
import com.aisafe.business.entity.BizRiskReportUploadDetail;
import com.aisafe.business.mapper.BizRiskReportUploadBatchMapper;
import com.aisafe.business.mapper.BizRiskReportUploadDetailMapper;
import com.aisafe.business.service.RiskClueService;
import com.aisafe.business.service.RiskReportUploadService;
import com.aisafe.business.support.RiskClueCategoryValidator;
import com.aisafe.business.support.RiskReportClueBuilder;
import com.aisafe.business.support.RiskReportExcelParser;
import com.aisafe.business.support.RiskReportFileStorage;
import com.aisafe.business.support.RiskReportImportError;
import com.aisafe.common.exception.BusinessException;
import com.aisafe.system.entity.SysUser;
import com.aisafe.system.service.ISysUserService;
import com.aisafe.system.service.ITagCategoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RiskReportUploadServiceImpl implements RiskReportUploadService {

    private static final Logger log = LoggerFactory.getLogger(RiskReportUploadServiceImpl.class);
    private static final int MAX_ROWS = 1000;

    private final BizRiskReportUploadBatchMapper batchMapper;
    private final BizRiskReportUploadDetailMapper detailMapper;
    private final RiskClueService riskClueService;
    private final ITagCategoryService tagCategoryService;
    private final RiskReportClueBuilder clueBuilder;
    private final RiskReportFileStorage fileStorage;
    private final ISysUserService userService;
    private final RiskReportUploadAsyncProcessor asyncProcessor;

    public RiskReportUploadServiceImpl(BizRiskReportUploadBatchMapper batchMapper,
                                       BizRiskReportUploadDetailMapper detailMapper,
                                       RiskClueService riskClueService,
                                       ITagCategoryService tagCategoryService,
                                       RiskReportClueBuilder clueBuilder,
                                       RiskReportFileStorage fileStorage,
                                       ISysUserService userService,
                                       @Lazy RiskReportUploadAsyncProcessor asyncProcessor) {
        this.batchMapper = batchMapper;
        this.detailMapper = detailMapper;
        this.riskClueService = riskClueService;
        this.tagCategoryService = tagCategoryService;
        this.clueBuilder = clueBuilder;
        this.fileStorage = fileStorage;
        this.userService = userService;
        this.asyncProcessor = asyncProcessor;
    }

    @Override
    public Map<String, Object> startUpload(MultipartFile file, SysUser user, String submitOrgName) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择上传文件");
        }
        String originalName = file.getOriginalFilename();
        if (!StringUtils.hasText(originalName) || !originalName.toLowerCase().endsWith(".xlsx")) {
            throw new BusinessException("仅支持 .xlsx 格式文件");
        }

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
                    processRow(batch, row, categoryValidator, loginUser);
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
                            SysUser loginUser) {
        RiskReportImportError error = clueBuilder.validateRowFields(row, categoryValidator);
        if (error != null) {
            throw new RowImportException(error);
        }
        BizRiskClue clue = clueBuilder.buildPendingClue(row, loginUser, batch.getSubmitTime());
        // 批量上传归属当前上传人所在部门，确保在「我的报送」中可见
        if (StringUtils.hasText(batch.getSubmitOrgName())) {
            clue.setSubmitOrgName(batch.getSubmitOrgName());
        }
        String clueId = riskClueService.save(clue);
        saveSuccessDetail(batch.getId(), row, clueId);
    }

    private void saveSuccessDetail(Long batchId, RiskReportExcelRow row, String clueId) {
        BizRiskReportUploadDetail detail = new BizRiskReportUploadDetail();
        detail.setBatchId(batchId);
        detail.setSerialNo(row.getNumber());
        detail.setEventName(trimEventName(row.getEventName()));
        detail.setStatus("success");
        detail.setClueId(clueId);
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
        if (!superAdmin && StringUtils.hasText(submitOrgName)
                && StringUtils.hasText(batch.getSubmitOrgName())
                && !submitOrgName.equals(batch.getSubmitOrgName())) {
            throw new BusinessException("无权查看该上传批次");
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

    private static class RowImportException extends RuntimeException {
        private final RiskReportImportError error;

        RowImportException(RiskReportImportError error) {
            super(error.getErrorMessage());
            this.error = error;
        }
    }
}
