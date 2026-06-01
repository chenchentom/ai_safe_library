package com.aisafe.business.service.impl;

import com.aisafe.business.document.BizRiskClue;
import com.aisafe.business.dto.RiskClueSearchQuery;
import com.aisafe.business.service.RiskClueService;
import com.aisafe.business.service.RiskReportService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 风险报送 Service 实现
 */
@Service
public class RiskReportServiceImpl implements RiskReportService {

    private final RiskClueService riskClueService;

    public RiskReportServiceImpl(RiskClueService riskClueService) {
        this.riskClueService = riskClueService;
    }

    @Override
    public void batchImport(List<BizRiskClue> clues, String reportUnit) {
        LocalDateTime now = LocalDateTime.now();
        for (BizRiskClue clue : clues) {
            clue.setSourceType("report");
            clue.setReviewStatus(10); // 待审核
            clue.setReportUnit(reportUnit);
            if (clue.getCreatedTime() == null) {
                clue.setCreatedTime(now);
            }
            if (clue.getUpdatedTime() == null) {
                clue.setUpdatedTime(now);
            }
            riskClueService.save(clue);
        }
    }

    @Override
    public Map<String, Object> getMyReports(String reportUnit, int page, int size) {
        RiskClueSearchQuery query = new RiskClueSearchQuery();
        query.setReportUnit(reportUnit);
        query.setPage(page);
        query.setSize(size);
        return riskClueService.search(query);
    }

    @Override
    public Map<String, Object> search(RiskClueSearchQuery query, String reportUnit) {
        query.setReportUnit(reportUnit);
        return riskClueService.search(query);
    }

    @Override
    public Map<String, Object> getStats(String reportUnit) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", countByQuery(reportUnit, null, null, null));
        stats.put("pending", countByQuery(reportUnit, 10, null, null));
        stats.put("reviewed", countByQuery(reportUnit, 20, null, null));
        stats.put("warehoused", countByQuery(reportUnit, 20, 1, null));
        return stats;
    }

    @Override
    public Map<String, Object> searchShared(RiskClueSearchQuery query) {
        query.setIsShared(1);
        return riskClueService.search(query);
    }

    @Override
    public Map<String, Object> getSharedStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", countByQuery(null, null, null, 1));
        stats.put("pending", countByQuery(null, 10, null, 1));
        stats.put("reviewed", countByQuery(null, 20, null, 1));
        stats.put("warehoused", countByQuery(null, 20, 1, 1));
        return stats;
    }

    private long countByQuery(String reportUnit, Integer reviewStatus, Integer isWarehouse, Integer isShared) {
        RiskClueSearchQuery query = new RiskClueSearchQuery();
        if (reportUnit != null) {
            query.setReportUnit(reportUnit);
        }
        if (isShared != null) {
            query.setIsShared(isShared);
        }
        query.setReviewStatus(reviewStatus);
        query.setIsWarehouse(isWarehouse);
        query.setPage(1);
        query.setSize(1);
        Object total = riskClueService.search(query).get("total");
        if (total instanceof Number) {
            return ((Number) total).longValue();
        }
        return 0L;
    }
}
