package com.aisafe.business.controller;

import com.aisafe.business.document.BizRiskClue;
import com.aisafe.business.document.BizRiskReviewRecord;
import com.aisafe.business.dto.ReviewDTO;
import com.aisafe.business.dto.RiskClueSearchQuery;
import com.aisafe.business.service.RiskClueService;
import com.aisafe.business.service.RiskReviewService;
import com.aisafe.common.result.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 风险线索控制器
 */
@RestController
@RequestMapping("/business/risk-clue")
public class RiskClueController {

    private static final Logger logger = LoggerFactory.getLogger(RiskClueController.class);

    private final RiskClueService riskClueService;
    private final RiskReviewService riskReviewService;

    public RiskClueController(RiskClueService riskClueService,
                              RiskReviewService riskReviewService) {
        this.riskClueService = riskClueService;
        this.riskReviewService = riskReviewService;
    }

    /**
     * 搜索风险线索（主线索库，所有用户可见全部线索）
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
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        RiskClueSearchQuery query = buildSearchQuery(
                keyword, riskCategory, reviewStatus, sourceWebsite, operatingEntity,
                submissionChannel, productsComponentsServices, submissionStartTime, submissionEndTime,
                isWarehouse, auditRiskCategory, operatingEntityHuman, auditUserName,
                auditStartTime, auditEndTime,
                sourceType, startTime, endTime, page, size, null);
        return R.ok(riskClueService.search(query));
    }

    @PostMapping("/search")
    public R<Map<String, Object>> searchPost(@RequestBody(required = false) Map<String, Object> body) {
        if (body == null) {
            body = Map.of();
        }
        RiskClueSearchQuery query = buildSearchQuery(
                (String) body.get("keyword"),
                (String) body.get("riskCategory"),
                body.get("reviewStatus"),
                (String) body.get("sourceWebsite"),
                (String) body.get("operatingEntity"),
                (String) body.get("submissionChannel"),
                (String) body.get("productsComponentsServices"),
                firstNonBlank((String) body.get("submissionStartTime"), (String) body.get("startTime")),
                firstNonBlank((String) body.get("submissionEndTime"), (String) body.get("endTime")),
                body.get("isWarehouse"),
                (String) body.get("auditRiskCategory"),
                (String) body.get("operatingEntityHuman"),
                (String) body.get("auditUserName"),
                (String) body.get("auditStartTime"),
                (String) body.get("auditEndTime"),
                (String) body.get("sourceType"),
                (String) body.get("startTime"),
                (String) body.get("endTime"),
                body.get("page") != null ? ((Number) body.get("page")).intValue() : 1,
                body.get("size") != null ? ((Number) body.get("size")).intValue() : 10,
                null);
        return R.ok(riskClueService.search(query));
    }

    private RiskClueSearchQuery buildSearchQuery(
            String keyword,
            String riskCategory,
            Object reviewStatus,
            String sourceWebsite,
            String operatingEntity,
            String submissionChannel,
            String productsComponentsServices,
            String submissionStartTime,
            String submissionEndTime,
            Object isWarehouse,
            String auditRiskCategory,
            String operatingEntityHuman,
            String auditUserName,
            String auditStartTime,
            String auditEndTime,
            String sourceType,
            String legacyStartTime,
            String legacyEndTime,
            int page,
            int size,
            String reportUnit) {

        RiskClueSearchQuery query = new RiskClueSearchQuery();
        query.setKeyword(keyword);
        query.setRiskCategory(riskCategory);
        query.setReviewStatus(parseReviewStatus(reviewStatus));
        query.setSourceWebsite(sourceWebsite);
        query.setOperatingEntity(operatingEntity);
        query.setSubmissionChannel(submissionChannel);
        query.setProductsComponentsServices(productsComponentsServices);
        query.setSubmissionStartTime(firstNonBlank(submissionStartTime, legacyStartTime));
        query.setSubmissionEndTime(firstNonBlank(submissionEndTime, legacyEndTime));
        query.setIsWarehouse(parseIsWarehouse(isWarehouse));
        query.setAuditRiskCategory(auditRiskCategory);
        query.setOperatingEntityHuman(operatingEntityHuman);
        query.setAuditUserName(auditUserName);
        query.setAuditStartTime(auditStartTime);
        query.setAuditEndTime(auditEndTime);
        query.setSourceType(sourceType);
        query.setPage(page);
        query.setSize(size);
        query.setReportUnit(reportUnit);
        return query;
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.trim().isEmpty()) {
            return primary;
        }
        return fallback;
    }

    private Integer parseReviewStatus(Object reviewStatus) {
        if (reviewStatus == null) {
            return null;
        }
        if (reviewStatus instanceof Integer) {
            return (Integer) reviewStatus;
        }
        if (reviewStatus instanceof Number) {
            return ((Number) reviewStatus).intValue();
        }
        if (reviewStatus instanceof String) {
            String str = (String) reviewStatus;
            if (str.trim().isEmpty()) {
                return null;
            }
            try {
                return Integer.parseInt(str.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private Integer parseIsWarehouse(Object isWarehouse) {
        if (isWarehouse == null) {
            return null;
        }
        if (isWarehouse instanceof Boolean) {
            return Boolean.TRUE.equals(isWarehouse) ? 1 : 0;
        }
        return parseReviewStatus(isWarehouse);
    }

    @PutMapping
    public R<String> save(@RequestBody BizRiskClue clue) {
        String id = riskClueService.save(clue);
        return R.ok(id);
    }

    @GetMapping("/{id}")
    public R<BizRiskClue> getById(@PathVariable String id) {
        BizRiskClue clue = riskClueService.getById(id);
        if (clue == null) {
            return R.fail("线索不存在");
        }
        return R.ok(clue);
    }

    @PostMapping("/review")
    public R<String> review(@RequestBody ReviewDTO dto) {
        riskReviewService.review(dto);
        return R.ok("审核完成");
    }

    @GetMapping("/{id}/review-history")
    public R<List<BizRiskReviewRecord>> reviewHistory(@PathVariable String id) {
        List<BizRiskReviewRecord> records = riskReviewService.getReviewHistory(id);
        return R.ok(records);
    }

    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        long total = riskClueService.countAll();
        long pending = riskClueService.countByReviewStatus(10);
        long reviewed = riskClueService.countByReviewStatus(20);
        long rejected = riskClueService.countByReviewStatus(40);

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("pending", pending);
        stats.put("reviewed", reviewed);
        stats.put("rejected", rejected);
        return R.ok(stats);
    }

    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable String id) {
        riskClueService.deleteById(id);
        return R.ok("删除成功");
    }
}
