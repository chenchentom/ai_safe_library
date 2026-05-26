package com.aisafe.business.service;

import com.aisafe.business.document.BizRiskReviewRecord;
import com.aisafe.business.dto.ReviewDTO;

import java.util.List;

/**
 * 风险审核 Service 接口
 */
public interface RiskReviewService {

    /**
     * 审核线索并回写 ES 人工审核字段
     */
    void review(ReviewDTO dto);

    List<BizRiskReviewRecord> getReviewHistory(String clueId);
}
