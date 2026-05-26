package com.aisafe.business.repository;

import com.aisafe.business.document.BizRiskReviewRecord;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

/**
 * 风险审核记录 ES Repository
 */
public interface BizRiskReviewRecordRepository extends ElasticsearchRepository<BizRiskReviewRecord, String> {

    /**
     * 根据线索ID查询审核记录，按审核时间倒序
     */
    List<BizRiskReviewRecord> findByClueIdOrderByReviewTimeDesc(String clueId);
}
