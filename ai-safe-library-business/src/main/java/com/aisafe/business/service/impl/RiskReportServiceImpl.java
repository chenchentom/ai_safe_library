package com.aisafe.business.service.impl;

import com.aisafe.business.document.BizRiskClue;
import com.aisafe.business.service.RiskClueService;
import com.aisafe.business.service.RiskReportService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
        return riskClueService.search(null, null, null, "report", null, null, page, size, reportUnit);
    }
}
