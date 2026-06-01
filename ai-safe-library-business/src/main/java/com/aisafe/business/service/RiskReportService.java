package com.aisafe.business.service;

import com.aisafe.business.document.BizRiskClue;
import com.aisafe.business.dto.RiskClueSearchQuery;

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
     * 查询我的报送线索（按报送部门过滤）
     *
     * @param reportUnit 报送单位
     * @param page       页码（从1开始）
     * @param size       每页大小
     * @return {total, rows}
     */
    Map<String, Object> getMyReports(String reportUnit, int page, int size);

    /**
     * 多条件搜索本部门报送线索（强制按 submit_org_name 过滤）
     */
    Map<String, Object> search(RiskClueSearchQuery query, String reportUnit);

    /**
     * 本部门报送统计
     */
    Map<String, Object> getStats(String reportUnit);

    /**
     * 搜索已共享线索（强制 is_shared=1）
     */
    Map<String, Object> searchShared(RiskClueSearchQuery query);

    /**
     * 已共享线索统计
     */
    Map<String, Object> getSharedStats();
}
