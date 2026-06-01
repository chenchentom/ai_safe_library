package com.aisafe.business.controller;

import com.aisafe.business.document.BizRiskClue;
import com.aisafe.business.dto.RiskClueManualCreateDTO;
import com.aisafe.business.dto.RiskClueSearchQuery;
import com.aisafe.business.service.RiskClueManualService;
import com.aisafe.business.service.RiskClueService;
import com.aisafe.common.result.R;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 安全事件库（只读）：仅展示已审核且已入库（is_warehouse=1）的线索
 */
@RestController
@RequestMapping("/business/security-event")
public class SecurityEventController {

    private final RiskClueService riskClueService;
    private final RiskClueManualService riskClueManualService;

    public SecurityEventController(RiskClueService riskClueService,
                                   RiskClueManualService riskClueManualService) {
        this.riskClueService = riskClueService;
        this.riskClueManualService = riskClueManualService;
    }

    @GetMapping("/search")
    public R<Map<String, Object>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String auditRiskCategory,
            @RequestParam(required = false) String sourceWebsite,
            @RequestParam(required = false) String operatingEntityHuman,
            @RequestParam(required = false) String productsComponentsServices,
            @RequestParam(required = false) String auditUserName,
            @RequestParam(required = false) String auditStartTime,
            @RequestParam(required = false) String auditEndTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "16") int size) {

        RiskClueSearchQuery query = buildEventQuery(
                keyword, auditRiskCategory, sourceWebsite, operatingEntityHuman,
                productsComponentsServices, auditUserName, auditStartTime, auditEndTime,
                page, size);
        return R.ok(riskClueService.search(query));
    }

    @PostMapping("/search")
    public R<Map<String, Object>> searchPost(@RequestBody(required = false) Map<String, Object> body) {
        if (body == null) {
            body = Map.of();
        }
        RiskClueSearchQuery query = buildEventQuery(
                (String) body.get("keyword"),
                (String) body.get("auditRiskCategory"),
                (String) body.get("sourceWebsite"),
                (String) body.get("operatingEntityHuman"),
                (String) body.get("productsComponentsServices"),
                (String) body.get("auditUserName"),
                (String) body.get("auditStartTime"),
                (String) body.get("auditEndTime"),
                body.get("page") != null ? ((Number) body.get("page")).intValue() : 1,
                body.get("size") != null ? ((Number) body.get("size")).intValue() : 16);
        return R.ok(riskClueService.search(query));
    }

    @GetMapping("/{id}")
    public R<BizRiskClue> getById(@PathVariable String id) {
        BizRiskClue clue = riskClueService.getById(id);
        if (clue == null
                || clue.getAuditStatus() == null
                || clue.getAuditStatus() != 20
                || clue.getIsWarehouse() == null
                || clue.getIsWarehouse() != 1) {
            return R.fail("事件不存在或未入库");
        }
        return R.ok(clue);
    }

    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        return R.ok(Map.of("total", riskClueService.countWarehoused()));
    }

    /**
     * 手动新增安全事件（创建线索并自动审核入库）
     */
    @PostMapping
    public R<Map<String, String>> create(@RequestBody RiskClueManualCreateDTO dto) {
        String id = riskClueManualService.createEvent(dto);
        return R.ok(Map.of("id", id));
    }

    /**
     * 切换共享状态（已共享 ↔ 未共享）
     */
    @PutMapping("/{id}/share")
    public R<Map<String, Object>> toggleShare(@PathVariable String id) {
        return R.ok(riskClueService.toggleEventShare(id));
    }

    private RiskClueSearchQuery buildEventQuery(
            String keyword,
            String auditRiskCategory,
            String sourceWebsite,
            String operatingEntityHuman,
            String productsComponentsServices,
            String auditUserName,
            String auditStartTime,
            String auditEndTime,
            int page,
            int size) {

        RiskClueSearchQuery query = new RiskClueSearchQuery();
        query.setKeyword(keyword);
        query.setReviewStatus(20);
        query.setIsWarehouse(1);
        query.setAuditRiskCategory(auditRiskCategory);
        query.setSourceWebsite(sourceWebsite);
        query.setOperatingEntityHuman(operatingEntityHuman);
        query.setProductsComponentsServices(productsComponentsServices);
        query.setAuditUserName(auditUserName);
        query.setAuditStartTime(auditStartTime);
        query.setAuditEndTime(auditEndTime);
        query.setSortField("audit_time");
        query.setPage(page);
        query.setSize(size);
        return query;
    }
}
