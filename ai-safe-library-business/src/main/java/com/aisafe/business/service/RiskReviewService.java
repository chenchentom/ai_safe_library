package com.aisafe.business.service;

import com.aisafe.business.document.BizRiskReviewRecord;
import com.aisafe.business.dto.BatchReviewResult;
import com.aisafe.business.dto.ReviewDTO;
import com.aisafe.business.dto.RiskClueSearchQuery;

import java.util.List;

/**
 * 风险审核 Service 接口
 */
public interface RiskReviewService {

    /**
     * 审核线索并回写 ES 人工审核字段
     */
    void review(ReviewDTO dto);

    /**
     * 按筛选条件批量审核未审核线索，审核字段取自对应报送文本内容
     */
    BatchReviewResult batchReviewByQuery(RiskClueSearchQuery query, int batchIsWarehouse);

    List<BizRiskReviewRecord> getReviewHistory(String clueId);
}
