package com.aisafe.business.service;

import com.aisafe.business.document.BizRiskClue;

import java.util.List;
import java.util.Map;

/**
 * 风险报送 Service 接口
 */
public interface RiskReportService {

    /**
     * 批量导入报送线索
     *
     * @param clues      线索列表
     * @param reportUnit 报送单位
     */
    void batchImport(List<BizRiskClue> clues, String reportUnit);

    /**
     * 查询我的报送线索
     *
     * @param reportUnit 报送单位
     * @param page       页码（从1开始）
     * @param size       每页大小
     * @return {total, rows}
     */
    Map<String, Object> getMyReports(String reportUnit, int page, int size);
}
