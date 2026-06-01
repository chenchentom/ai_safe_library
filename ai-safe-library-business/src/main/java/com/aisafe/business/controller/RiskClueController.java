package com.aisafe.business.controller;

import com.aisafe.business.document.BizRiskClue;
import com.aisafe.business.document.BizRiskReviewRecord;
import com.aisafe.business.dto.BatchReviewResult;
import com.aisafe.business.dto.ReviewDTO;
import com.aisafe.business.dto.RiskClueManualCreateDTO;
import com.aisafe.business.dto.RiskClueSearchQuery;
import com.aisafe.business.support.RiskClueSearchSupport;
import com.aisafe.business.service.RiskClueManualService;
import com.aisafe.business.service.RiskClueService;
import com.aisafe.business.service.RiskReviewService;
import com.aisafe.common.result.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
    private final RiskClueManualService riskClueManualService;

    public RiskClueController(RiskClueService riskClueService,
                              RiskReviewService riskReviewService,
                              RiskClueManualService riskClueManualService) {
        this.riskClueService = riskClueService;
        this.riskReviewService = riskReviewService;
        this.riskClueManualService = riskClueManualService;
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
            @RequestParam(required = false) String submitUserName,
            @RequestParam(required = false) String submitOrgName,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        RiskClueSearchQuery query = RiskClueSearchSupport.buildSearchQuery(
                keyword, riskCategory, reviewStatus, sourceWebsite, operatingEntity,
                submissionChannel, productsComponentsServices, submissionStartTime, submissionEndTime,
                isWarehouse, auditRiskCategory, operatingEntityHuman, auditUserName,
                auditStartTime, auditEndTime, submitUserName, submitOrgName,
                sourceType, startTime, endTime, page, size, null);
        return R.ok(riskClueService.search(query));
    }

    @PostMapping("/search")
    public R<Map<String, Object>> searchPost(@RequestBody(required = false) Map<String, Object> body) {
        RiskClueSearchQuery query = RiskClueSearchSupport.buildFromMap(body, null);
        return R.ok(riskClueService.search(query));
    }

    @PutMapping
    public R<String> save(@RequestBody BizRiskClue clue) {
        String id = riskClueService.save(clue);
        return R.ok(id);
    }

    /**
     * 根据报送人昵称解析报送部门（用于新增表单联动；路径须在 /{id} 之前且勿与线索 id 冲突）
     */
    @GetMapping("/manual/submit-org")
    public R<Map<String, String>> resolveSubmitOrg(@RequestParam String submitUserName) {
        String orgName = riskClueManualService.resolveSubmitOrgName(submitUserName);
        Map<String, String> result = new HashMap<>();
        result.put("submitOrgName", orgName != null ? orgName : "");
        return R.ok(result);
    }

    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        long total = riskClueService.countAll();
        long pending = riskClueService.countByReviewStatus(10);
        long reviewed = riskClueService.countByReviewStatus(20);
        long warehoused = riskClueService.countWarehoused();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("pending", pending);
        stats.put("reviewed", reviewed);
        stats.put("warehoused", warehoused);
        return R.ok(stats);
    }

    /**
     * 手动新增风险线索（待审核）
     */
    @PostMapping
    public R<Map<String, String>> create(@RequestBody RiskClueManualCreateDTO dto) {
        String id = riskClueManualService.createClue(dto);
        return R.ok(Map.of("id", id));
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

    /**
     * 批量审核：按当前筛选条件处理所有未审核线索，审核字段与报送文本内容一致
     */
    @PostMapping("/batch-review")
    public R<BatchReviewResult> batchReview(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> payload = body != null ? body : Map.of();
        Integer batchIsWarehouse = RiskClueSearchSupport.parseIsWarehouse(payload.get("batchIsWarehouse"));
        int warehouseDecision = batchIsWarehouse != null && batchIsWarehouse != 0 ? 1 : 0;

        Map<String, Object> filterBody = new HashMap<>(payload);
        filterBody.remove("batchIsWarehouse");
        RiskClueSearchQuery query = RiskClueSearchSupport.buildFromMap(filterBody, null);
        BatchReviewResult result = riskReviewService.batchReviewByQuery(query, warehouseDecision);
        return R.ok(result);
    }

    @GetMapping("/{id}/review-history")
    public R<List<BizRiskReviewRecord>> reviewHistory(@PathVariable String id) {
        List<BizRiskReviewRecord> records = riskReviewService.getReviewHistory(id);
        return R.ok(records);
    }

    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable String id) {
        riskClueService.deleteById(id);
        return R.ok("删除成功");
    }
}
