package com.aisafe.business.repository;

import com.aisafe.business.document.BizRiskClue;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 风险线索 ES Repository
 */
public interface BizRiskClueRepository extends ElasticsearchRepository<BizRiskClue, String> {
}
