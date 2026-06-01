package com.aisafe.business.service;

import com.aisafe.business.document.BizRiskClue;
import com.aisafe.business.dto.RiskClueSearchQuery;

import java.util.Map;

/**
 * 风险线索 Service 接口
 */
public interface RiskClueService {

    /**
     * 多条件搜索风险线索
     */
    Map<String, Object> search(RiskClueSearchQuery query);

    /**
     * @deprecated 请使用 {@link #search(RiskClueSearchQuery)}
     */
    @Deprecated
    default Map<String, Object> search(String keyword, String riskLevel, Integer reviewStatus,
                                       String sourceType, String startTime, String endTime,
                                       int page, int size, String reportUnit) {
        RiskClueSearchQuery query = new RiskClueSearchQuery();
        query.setKeyword(keyword);
        query.setReviewStatus(reviewStatus);
        query.setSourceType(sourceType);
        query.setSubmissionStartTime(startTime);
        query.setSubmissionEndTime(endTime);
        query.setPage(page);
        query.setSize(size);
        query.setReportUnit(reportUnit);
        return search(query);
    }

    BizRiskClue getById(String id);

    String save(BizRiskClue clue);

    void updateStatus(String id, Integer reviewStatus);

    void deleteById(String id);

    /**
     * 更新待审核线索的报送基础字段（不修改审核侧字段与报送时间）
     */
    void updatePendingSubmission(BizRiskClue clue);

    /**
     * 切换安全事件共享状态（仅已入库事件）
     * @return isShared、shareTime
     */
    Map<String, Object> toggleEventShare(String id);

    long countByReviewStatus(Integer reviewStatus);

    long countAll();

    /** 已入库安全事件数量（audit_status=20 且 is_warehouse=1） */
    long countWarehoused();
}
