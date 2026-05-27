package com.aisafe.business.service.impl;

import com.aisafe.business.document.BizRiskClue;
import com.aisafe.business.dto.RiskClueSearchQuery;
import com.aisafe.business.repository.BizRiskClueRepository;
import com.aisafe.business.service.RiskClueService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 风险线索 Service 实现
 */
@Service
public class RiskClueServiceImpl implements RiskClueService {

    private final BizRiskClueRepository bizRiskClueRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    public RiskClueServiceImpl(BizRiskClueRepository bizRiskClueRepository,
                               ElasticsearchOperations elasticsearchOperations) {
        this.bizRiskClueRepository = bizRiskClueRepository;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @Override
    public Map<String, Object> search(RiskClueSearchQuery query) {
        final RiskClueSearchQuery searchQuery = query != null ? query : new RiskClueSearchQuery();

        NativeQueryBuilder builder = NativeQuery.builder();
        int page = Math.max(searchQuery.getPage(), 1);
        int size = Math.max(searchQuery.getSize(), 1);

        builder.withQuery(q -> q.bool(b -> {
            if (hasText(searchQuery.getKeyword())) {
                b.must(m -> m.multiMatch(mm -> mm
                        .fields("event_name", "content", "risk_description")
                        .query(searchQuery.getKeyword().trim())));
            }

            addRiskCategoryFilter(b, searchQuery.getRiskCategory(), "class_report_1", "class_report_2", "class_report_list");

            if (searchQuery.getReviewStatus() != null) {
                b.filter(f -> f.term(t -> t.field("audit_status").value(searchQuery.getReviewStatus())));
            }

            if (searchQuery.getIsWarehouse() != null) {
                addWarehouseFilter(b, searchQuery.getIsWarehouse());
            }

            addRiskCategoryFilter(b, searchQuery.getAuditRiskCategory(), "class_human_1", "class_human_2", "class_human_list");
            addKeywordLikeFilter(b, "operating_entity_human", searchQuery.getOperatingEntityHuman());
            addKeywordLikeFilter(b, "audit_user_name", searchQuery.getAuditUserName());

            addDateRangeFilter(
                    b,
                    "audit_time",
                    normalizeStartTime(searchQuery.getAuditStartTime()),
                    normalizeEndTime(searchQuery.getAuditEndTime()));

            addKeywordLikeFilter(b, "source_website", searchQuery.getSourceWebsite());
            addKeywordLikeFilter(b, "operating_entity", searchQuery.getOperatingEntity());
            addKeywordLikeFilter(b, "submission_channel", searchQuery.getSubmissionChannel());

            if (hasText(searchQuery.getProductsComponentsServices())) {
                String value = searchQuery.getProductsComponentsServices().trim();
                b.filter(f -> f.bool(bb -> bb
                        .should(s -> s.match(m -> m.field("products_components_services").query(value)))
                        .should(s -> s.wildcard(w -> w
                                .field("products_components_services")
                                .value("*" + escapeWildcard(value) + "*")))
                        .minimumShouldMatch("1")));
            }

            if (hasText(searchQuery.getSourceType())) {
                b.filter(f -> f.term(t -> t.field("submission_channel").value(searchQuery.getSourceType().trim())));
            }

            if (hasText(searchQuery.getReportUnit())) {
                b.filter(f -> f.term(t -> t.field("submit_org_name").value(searchQuery.getReportUnit().trim())));
            }

            addDateRangeFilter(
                    b,
                    "submission_time",
                    normalizeStartTime(searchQuery.getSubmissionStartTime()),
                    normalizeEndTime(searchQuery.getSubmissionEndTime()));

            return b;
        }));

        builder.withPageable(PageRequest.of(page - 1, size));
        String sortField = hasText(searchQuery.getSortField()) ? searchQuery.getSortField().trim() : "submission_time";
        builder.withSort(Sort.by(Sort.Direction.DESC, sortField));

        SearchHits<BizRiskClue> hits = elasticsearchOperations.search(builder.build(), BizRiskClue.class);

        List<BizRiskClue> rows = new ArrayList<>();
        for (SearchHit<BizRiskClue> hit : hits) {
            rows.add(hit.getContent());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", hits.getTotalHits());
        result.put("rows", rows);
        return result;
    }

    private void addRiskCategoryFilter(BoolQuery.Builder b, String riskCategory,
                                       String fieldLevel1, String fieldLevel2, String fieldList) {
        if (!hasText(riskCategory)) {
            return;
        }
        String trimmed = riskCategory.trim();
        int slashIndex = trimmed.indexOf('/');

        if (slashIndex > 0) {
            String level1 = trimmed.substring(0, slashIndex).trim();
            String level2 = trimmed.substring(slashIndex + 1).trim();
            if (hasText(level1) && hasText(level2)) {
                String fullPath = level1 + "/" + level2;
                b.filter(f -> f.bool(bb -> bb
                        .should(s -> s.term(t -> t.field(fieldList).value(fullPath)))
                        .should(s -> s.bool(inner -> inner
                                .must(m -> m.term(t -> t.field(fieldLevel1).value(level1)))
                                .must(m -> m.term(t -> t.field(fieldLevel2).value(level2)))))
                        .minimumShouldMatch("1")));
                return;
            }
        }

        b.filter(f -> f.term(t -> t.field(fieldLevel1).value(trimmed)));
    }

    private void addKeywordLikeFilter(BoolQuery.Builder b, String field, String value) {
        if (!hasText(value)) {
            return;
        }
        String trimmed = value.trim();
        b.filter(f -> f.bool(bb -> bb
                .should(s -> s.wildcard(w -> w.field(field).value("*" + escapeWildcard(trimmed) + "*")))
                .should(s -> s.term(t -> t.field(field).value(trimmed)))
                .minimumShouldMatch("1")));
    }

    private void addDateRangeFilter(BoolQuery.Builder b, String field, String startTime, String endTime) {
        if (!hasText(startTime) && !hasText(endTime)) {
            return;
        }
        b.filter(f -> f.range(r -> {
            r.field(field);
            if (hasText(startTime)) {
                r.gte(co.elastic.clients.json.JsonData.of(startTime));
            }
            if (hasText(endTime)) {
                r.lte(co.elastic.clients.json.JsonData.of(endTime));
            }
            return r;
        }));
    }

    private String normalizeStartTime(String raw) {
        if (!hasText(raw)) {
            return null;
        }
        String value = raw.trim();
        if (value.length() == 10) {
            return value + " 00:00:00";
        }
        return value;
    }

    private String normalizeEndTime(String raw) {
        if (!hasText(raw)) {
            return null;
        }
        String value = raw.trim();
        if (value.length() == 10) {
            return value + " 23:59:59";
        }
        return value;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String escapeWildcard(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("*", "\\*")
                .replace("?", "\\?");
    }

    @Override
    public BizRiskClue getById(String id) {
        Optional<BizRiskClue> optional = bizRiskClueRepository.findById(id);
        return optional.orElse(null);
    }

    @Override
    public String save(BizRiskClue clue) {
        if (clue.getCreatedTime() == null) {
            clue.setCreatedTime(LocalDateTime.now());
        }
        if (clue.getUpdatedTime() == null) {
            clue.setUpdatedTime(LocalDateTime.now());
        }
        BizRiskClue saved = bizRiskClueRepository.save(clue);
        return saved.getId();
    }

    @Override
    public void updateStatus(String id, Integer reviewStatus) {
        Optional<BizRiskClue> optional = bizRiskClueRepository.findById(id);
        if (optional.isPresent()) {
            BizRiskClue clue = optional.get();
            clue.setReviewStatus(reviewStatus);
            clue.setUpdatedTime(LocalDateTime.now());
            bizRiskClueRepository.save(clue);
        }
    }

    @Override
    public void deleteById(String id) {
        bizRiskClueRepository.deleteById(id);
    }

    @Override
    public long countByReviewStatus(Integer reviewStatus) {
        NativeQuery countQuery = NativeQuery.builder()
                .withQuery(q -> q.term(t -> t.field("audit_status").value(reviewStatus)))
                .withMaxResults(1)
                .build();
        SearchHits<BizRiskClue> hits = elasticsearchOperations.search(countQuery, BizRiskClue.class);
        return hits.getTotalHits();
    }

    @Override
    public long countAll() {
        NativeQuery countQuery = NativeQuery.builder()
                .withQuery(q -> q.matchAll(m -> m))
                .withMaxResults(1)
                .build();
        SearchHits<BizRiskClue> hits = elasticsearchOperations.search(countQuery, BizRiskClue.class);
        return hits.getTotalHits();
    }

    @Override
    public long countWarehoused() {
        RiskClueSearchQuery query = new RiskClueSearchQuery();
        query.setReviewStatus(20);
        query.setIsWarehouse(1);
        query.setPage(1);
        query.setSize(1);
        Object total = search(query).get("total");
        if (total instanceof Number) {
            return ((Number) total).longValue();
        }
        return 0L;
    }

    /**
     * 入库状态筛选：兼容 is_warehouse / isWarehouse 及历史数据类型差异
     */
    private void addWarehouseFilter(BoolQuery.Builder b, int isWarehouse) {
        if (isWarehouse == 1) {
            b.filter(f -> f.bool(bb -> bb
                    .should(s -> s.term(t -> t.field("is_warehouse").value(1)))
                    .should(s -> s.term(t -> t.field("isWarehouse").value(1)))
                    .minimumShouldMatch("1")));
        } else {
            b.filter(f -> f.bool(bb -> bb
                    .should(s -> s.term(t -> t.field("is_warehouse").value(0)))
                    .should(s -> s.term(t -> t.field("isWarehouse").value(0)))
                    .minimumShouldMatch("1")));
        }
    }
}
