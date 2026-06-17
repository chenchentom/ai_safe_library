package com.aisafe.business.controller;

import com.aisafe.common.exception.BusinessException;
import com.aisafe.business.dto.RiskClueManualCreateDTO;
import com.aisafe.business.dto.RiskClueSearchQuery;
import com.aisafe.business.service.RiskClueManualService;
import com.aisafe.business.service.RiskReportService;
import com.aisafe.business.service.RiskReportUploadService;
import com.aisafe.business.support.ReportDeptSupport;
import com.aisafe.business.support.RiskClueSearchSupport;
import com.aisafe.common.result.R;
import com.aisafe.system.entity.SysUser;
import com.aisafe.system.service.ISysUserService;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 风险报送控制器
 */
@RestController
@RequestMapping("/business/risk-report")
public class RiskReportController {

    private final RiskReportService riskReportService;
    private final RiskReportUploadService uploadService;
    private final RiskClueManualService riskClueManualService;
    private final ISysUserService userService;
    private final ReportDeptSupport reportDeptSupport;

    @Value("${aisafe.file.risk-report-template-path:}")
    private String riskReportTemplatePath;

    public RiskReportController(RiskReportService riskReportService,
                                RiskReportUploadService uploadService,
                                RiskClueManualService riskClueManualService,
                                ISysUserService userService,
                                ReportDeptSupport reportDeptSupport) {
        this.riskReportService = riskReportService;
        this.uploadService = uploadService;
        this.riskClueManualService = riskClueManualService;
        this.userService = userService;
        this.reportDeptSupport = reportDeptSupport;
    }

    /**
     * 获取我的报送线索
     */
    @GetMapping("/my")
    public R<Map<String, Object>> myReports(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        String reportUnit = reportDeptSupport.requireReportUnit(requireCurrentUser());
        Map<String, Object> result = riskReportService.getMyReports(reportUnit, page, size);
        return R.ok(result);
    }

    /**
     * 多条件搜索本部门报送线索（与线索库筛选一致，强制按 submit_org_name 过滤）
     */
    @GetMapping("/search")
    public R<Map<String, Object>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String riskCategory,
            @RequestParam(required = false) Object reviewStatus,
            @RequestParam(required = false) String sourceWebsite,
            @RequestParam(required = false) String operatingEntity,
            @RequestParam(required = false) String submissionChannel,
            @RequestParam(required = false) String productsComponentsServices,
            @RequestParam(required = false) String submissionStartTime,
            @RequestParam(required = false) String submissionEndTime,
            @RequestParam(required = false) Object isWarehouse,
            @RequestParam(required = false) String auditRiskCategory,
            @RequestParam(required = false) String operatingEntityHuman,
            @RequestParam(required = false) String auditUserName,
            @RequestParam(required = false) String auditStartTime,
            @RequestParam(required = false) String auditEndTime,
            @RequestParam(required = false) String submitUserName,
            @RequestParam(required = false) String submitOrgName,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) Object isVerify,
            @RequestParam(required = false) Object isSubmit,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        String reportUnit = reportDeptSupport.requireReportUnit(requireCurrentUser());
        RiskClueSearchQuery query = RiskClueSearchSupport.buildSearchQuery(
                keyword, riskCategory, reviewStatus, sourceWebsite, operatingEntity,
                submissionChannel, productsComponentsServices, submissionStartTime, submissionEndTime,
                isWarehouse, auditRiskCategory, operatingEntityHuman, auditUserName,
                auditStartTime, auditEndTime, submitUserName, submitOrgName,
                sourceType, startTime, endTime, page, size, reportUnit);
        RiskClueSearchSupport.applyYesNoFilters(query, isVerify, isSubmit);
        return R.ok(riskReportService.search(query, reportUnit));
    }

    /**
     * 新增本部门待审核报送
     */
    @PostMapping
    public R<Map<String, String>> createMyReport(@RequestBody RiskClueManualCreateDTO dto) {
        SysUser user = requireCurrentUser();
        String reportUnit = reportDeptSupport.requireReportUnit(user);
        String id = riskClueManualService.createReport(dto, reportUnit);
        Map<String, String> result = new HashMap<>();
        result.put("id", id);
        return R.ok(result);
    }

    /**
     * 编辑本部门报送的基础信息（待审核/已审核）
     */
    @PutMapping("/{id}")
    public R<String> updateMyReport(@PathVariable String id, @RequestBody RiskClueManualCreateDTO dto) {
        String reportUnit = reportDeptSupport.requireReportUnit(requireCurrentUser());
        riskClueManualService.updatePendingReport(id, dto, reportUnit);
        return R.ok("更新成功");
    }

    /**
     * 下载批量上传 Excel 模板（路径见 aisafe.file.risk-report-template-path）
     */
    @GetMapping("/template")
    public ResponseEntity<Resource> downloadTemplate() {
        if (riskReportTemplatePath == null || riskReportTemplatePath.isBlank()) {
            throw new BusinessException("未配置批量上传模板路径");
        }

        Path path = Paths.get(riskReportTemplatePath.trim());
        if (!Files.isRegularFile(path)) {
            throw new BusinessException("模板文件不存在，请检查配置: " + riskReportTemplatePath);
        }

        Resource resource = new FileSystemResource(path);
        String fileName = path.getFileName().toString();
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .body(resource);
    }

    /**
     * 校验 Excel + 可选 ZIP 匹配（不落库）
     */
    @PostMapping("/upload/preview")
    public R<Map<String, Object>> previewUpload(
            @RequestParam("excel") MultipartFile excel,
            @RequestParam(value = "zip", required = false) MultipartFile zip) {
        SysUser user = requireCurrentUser();
        String reportUnit = reportDeptSupport.requireReportUnit(user);
        return R.ok(uploadService.previewUpload(excel, zip, user, reportUnit));
    }

    /**
     * 确认批量导入
     */
    @PostMapping("/upload/confirm")
    public R<Map<String, Object>> confirmUpload(@RequestParam("previewToken") String previewToken) {
        SysUser user = requireCurrentUser();
        String reportUnit = reportDeptSupport.requireReportUnit(user);
        return R.ok(uploadService.confirmUpload(previewToken, user, reportUnit));
    }

    /**
     * 上传 Excel 批量导入报送线索（异步处理，返回批次 ID 供轮询进度）
     */
    @PostMapping("/upload")
    public R<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                         @RequestParam(value = "zip", required = false) MultipartFile zip) {
        SysUser user = requireCurrentUser();
        String reportUnit = reportDeptSupport.requireReportUnit(user);
        return R.ok(uploadService.startUpload(file, zip, user, reportUnit));
    }

    /**
     * 查询上传处理进度
     */
    @GetMapping("/upload/batches/{batchId}/progress")
    public R<Map<String, Object>> uploadProgress(@PathVariable Long batchId) {
        SysUser user = requireCurrentUser();
        boolean superAdmin = reportDeptSupport.isSuperAdmin(user);
        String reportUnit = resolveUploadReportUnit(user, superAdmin);
        return R.ok(uploadService.getProgress(batchId, user, reportUnit, superAdmin));
    }

    /**
     * 上传历史列表
     */
    @GetMapping("/upload/batches")
    public R<Map<String, Object>> uploadBatches(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String submitUserName,
            @RequestParam(required = false) String submitTimeStart,
            @RequestParam(required = false) String submitTimeEnd) {
        SysUser user = requireCurrentUser();
        boolean superAdmin = reportDeptSupport.isSuperAdmin(user);
        String reportUnit = resolveUploadReportUnit(user, superAdmin);
        return R.ok(uploadService.listBatches(page, size, status, keyword, submitUserName,
                submitTimeStart, submitTimeEnd, user, reportUnit, superAdmin));
    }

    /**
     * 上传批次详情
     */
    @GetMapping("/upload/batches/{batchId}")
    public R<Map<String, Object>> uploadBatchDetail(@PathVariable Long batchId) {
        SysUser user = requireCurrentUser();
        boolean superAdmin = reportDeptSupport.isSuperAdmin(user);
        String reportUnit = resolveUploadReportUnit(user, superAdmin);
        return R.ok(uploadService.getBatch(batchId, user, reportUnit, superAdmin));
    }

    /**
     * 上传失败/成功明细
     */
    @GetMapping("/upload/batches/{batchId}/details")
    public R<Map<String, Object>> uploadBatchDetails(
            @PathVariable Long batchId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        SysUser user = requireCurrentUser();
        boolean superAdmin = reportDeptSupport.isSuperAdmin(user);
        String reportUnit = resolveUploadReportUnit(user, superAdmin);
        return R.ok(uploadService.listDetails(batchId, page, size, status, user, reportUnit, superAdmin));
    }

    /**
     * 获取我的报送统计
     */
    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        String reportUnit = reportDeptSupport.requireReportUnit(requireCurrentUser());
        return R.ok(riskReportService.getStats(reportUnit));
    }

    /**
     * 多条件搜索已共享线索（强制 is_shared=1，跨部门可见）
     */
    @GetMapping("/shared/search")
    public R<Map<String, Object>> searchShared(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String riskCategory,
            @RequestParam(required = false) Object reviewStatus,
            @RequestParam(required = false) String sourceWebsite,
            @RequestParam(required = false) String operatingEntity,
            @RequestParam(required = false) String submissionChannel,
            @RequestParam(required = false) String productsComponentsServices,
            @RequestParam(required = false) String submissionStartTime,
            @RequestParam(required = false) String submissionEndTime,
            @RequestParam(required = false) Object isWarehouse,
            @RequestParam(required = false) String auditRiskCategory,
            @RequestParam(required = false) String operatingEntityHuman,
            @RequestParam(required = false) String auditUserName,
            @RequestParam(required = false) String auditStartTime,
            @RequestParam(required = false) String auditEndTime,
            @RequestParam(required = false) String submitUserName,
            @RequestParam(required = false) String submitOrgName,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) Object isVerify,
            @RequestParam(required = false) Object isSubmit,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        RiskClueSearchQuery query = RiskClueSearchSupport.buildSearchQuery(
                keyword, riskCategory, reviewStatus, sourceWebsite, operatingEntity,
                submissionChannel, productsComponentsServices, submissionStartTime, submissionEndTime,
                isWarehouse, auditRiskCategory, operatingEntityHuman, auditUserName,
                auditStartTime, auditEndTime, submitUserName, submitOrgName,
                sourceType, startTime, endTime, page, size, null);
        RiskClueSearchSupport.applyYesNoFilters(query, isVerify, isSubmit);
        return R.ok(riskReportService.searchShared(query));
    }

    /**
     * 已共享线索统计
     */
    @GetMapping("/shared/stats")
    public R<Map<String, Object>> sharedStats() {
        return R.ok(riskReportService.getSharedStats());
    }

    private SysUser requireCurrentUser() {
        SysUser user = userService.getById(StpUtil.getLoginIdAsLong());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    /** 上传批次类接口：超管可不按部门过滤，普通用户必须有部门 */
    private String resolveUploadReportUnit(SysUser user, boolean superAdmin) {
        if (superAdmin) {
            return reportDeptSupport.resolveDeptName(user);
        }
        return reportDeptSupport.requireReportUnit(user);
    }
}
