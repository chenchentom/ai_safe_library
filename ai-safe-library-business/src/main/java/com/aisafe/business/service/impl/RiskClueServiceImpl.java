package com.aisafe.business.service.impl;

import com.aisafe.business.document.BizRiskClue;
import com.aisafe.business.dto.RiskClueSearchQuery;
import com.aisafe.business.repository.BizRiskClueRepository;
import com.aisafe.business.service.RiskClueService;
import com.aisafe.common.enums.BusinessType;
import com.aisafe.system.service.AuditLogService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.query.ScriptType;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.stereotype.Service;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 风险线索 Service 实现
 */
@Service
public class RiskClueServiceImpl implements RiskClueService {

    private static final DateTimeFormatter ES_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BizRiskClueRepository bizRiskClueRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final AuditLogService auditLogService;

    public RiskClueServiceImpl(BizRiskClueRepository bizRiskClueRepository,
                               ElasticsearchOperations elasticsearchOperations,
                               AuditLogService auditLogService) {
        this.bizRiskClueRepository = bizRiskClueRepository;
        this.elasticsearchOperations = elasticsearchOperations;
        this.auditLogService = auditLogService;
    }

    @Override
    public Map<String, Object> search(RiskClueSearchQuery query) {
        final RiskClueSearchQuery searchQuery = query != null ? query : new RiskClueSearchQuery();

        NativeQueryBuilder builder = NativeQuery.builder();
        int page = Math.max(searchQuery.getPage(), 1);
        int size = Math.max(searchQuery.getSize(), 1);

        builder.withQuery(q -> q.bool(b -> {
            if (hasText(searchQuery.getKeyword())) {
                String keyword = searchQuery.getKeyword().trim();
                // 多词检索要求同一字段内全部词都命中，避免搜 "hermes agent" 却返回仅含 agent 的文档
                b.must(m -> m.multiMatch(mm -> mm
                        .fields("event_name", "content", "risk_description")
                        .query(keyword)
                        .type(TextQueryType.BestFields)
                        .operator(Operator.And)));
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
            addKeywordLikeFilter(b, "submit_user_name", searchQuery.getSubmitUserName());
            addKeywordLikeFilter(b, "submit_org_name", searchQuery.getSubmitOrgName());

            if (hasText(searchQuery.getProductsComponentsServices())) {
                String value = searchQuery.getProductsComponentsServices().trim();
                b.filter(f -> f.bool(bb -> bb
                        .should(s -> s.match(m -> m
                                .field("products_components_services")
                                .query(value)
                                .operator(Operator.And)))
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

            if (searchQuery.getIsShared() != null) {
                b.filter(f -> f.term(t -> t.field("is_shared").value(searchQuery.getIsShared())));
            }

            addDateRangeFilter(
                    b,
                    "submission_time",
                    normalizeStartTime(searchQuery.getSubmissionStartTime()),
                    normalizeEndTime(searchQuery.getSubmissionEndTime()));

            return b;
        }));

        builder.withPageable(PageRequest.of(page - 1, size));
        String sortField = hasText(searchQuery.getSortField()) ? searchQuery.getSortField().trim() : "create_time";
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
        String[] segments = riskCategory.split(",");
        java.util.List<String> categories = new java.util.ArrayList<>();
        for (String segment : segments) {
            if (hasText(segment)) {
                categories.add(segment.trim());
            }
        }
        if (categories.isEmpty()) {
            return;
        }
        b.filter(f -> f.bool(outer -> {
            for (String category : categories) {
                String trimmed = category;
                outer.should(s -> {
                    int slashIndex = trimmed.indexOf('/');
                    if (slashIndex > 0) {
                        String level1 = trimmed.substring(0, slashIndex).trim();
                        String level2 = trimmed.substring(slashIndex + 1).trim();
                        if (hasText(level1) && hasText(level2)) {
                            String fullPath = level1 + "/" + level2;
                            return s.bool(inner -> inner
                                    .should(sh -> sh.term(t -> t.field(fieldList).value(fullPath)))
                                    .should(sh -> sh.bool(n -> n
                                            .must(m -> m.term(t -> t.field(fieldLevel1).value(level1)))
                                            .must(m -> m.term(t -> t.field(fieldLevel2).value(level2)))))
                                    .minimumShouldMatch("1"));
                        }
                    }
                    return s.term(t -> t.field(fieldLevel1).value(trimmed));
                });
            }
            outer.minimumShouldMatch("1");
            return outer;
        }));
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
        LocalDateTime now = LocalDateTime.now();
        if (clue.getCreateTime() == null) {
            clue.setCreateTime(now);
        }
        if (clue.getUpdateTime() == null) {
            clue.setUpdateTime(now);
        }
        String id = clue.getId();
        if (!hasText(id)) {
            id = UUID.randomUUID().toString();
            clue.setId(id);
        }
        Document doc = buildClueDocument(clue);
        UpdateQuery updateQuery = UpdateQuery.builder(id)
                .withDocument(doc)
                .withUpsert(doc)
                .build();
        elasticsearchOperations.update(updateQuery, elasticsearchOperations.getIndexCoordinatesFor(BizRiskClue.class));
        return id;
    }

    @Override
    public void updateStatus(String id, Integer reviewStatus) {
        if (!hasText(id)) {
            return;
        }
        Document doc = Document.create();
        doc.put("audit_status", reviewStatus);
        doc.put("update_time", LocalDateTime.now().format(ES_DATE_TIME));
        UpdateQuery updateQuery = UpdateQuery.builder(id)
                .withDocument(doc)
                .build();
        elasticsearchOperations.update(updateQuery, elasticsearchOperations.getIndexCoordinatesFor(BizRiskClue.class));
    }

    @Override
    public void updatePendingSubmission(BizRiskClue clue) {
        if (clue == null || !hasText(clue.getId())) {
            throw new com.aisafe.common.exception.BusinessException("线索ID不能为空");
        }
        if (!hasText(clue.getEventName())) {
            throw new com.aisafe.common.exception.BusinessException("请填写事件名称");
        }
        if (!hasText(clue.getRiskDescription())) {
            throw new com.aisafe.common.exception.BusinessException("请填写风险描述");
        }

        LocalDateTime now = LocalDateTime.now();
        Document doc = Document.create();
        doc.put("event_name", clue.getEventName().trim());
        doc.put("risk_description", clue.getRiskDescription().trim());
        putOptionalText(doc, "content", clue.getContent());
        putOptionalText(doc, "source_url", clue.getSourceUrl());
        putOptionalText(doc, "source_website", clue.getSourceWebsite());
        putOptionalText(doc, "paper_title", clue.getPaperTitle());
        putOptionalText(doc, "research_team", clue.getResearchTeam());
        putOptionalText(doc, "submission_channel", clue.getSubmissionChannel());
        putOptionalText(doc, "operating_entity", clue.getOperatingEntity());
        putOptionalText(doc, "products_components_services", clue.getProductsComponentsServices());
        putOptionalText(doc, "class_report_1", clue.getClassReport1());
        putOptionalText(doc, "class_report_2", clue.getClassReport2());
        if (clue.getClassReportList() != null && !clue.getClassReportList().isEmpty()) {
            doc.put("class_report_list", clue.getClassReportList());
        } else {
            doc.put("class_report_list", null);
        }
        doc.put("update_time", now.format(ES_DATE_TIME));

        UpdateQuery updateQuery = UpdateQuery.builder(clue.getId().trim())
                .withDocument(doc)
                .build();
        elasticsearchOperations.update(updateQuery, elasticsearchOperations.getIndexCoordinatesFor(BizRiskClue.class));
    }

    private Document buildClueDocument(BizRiskClue clue) {
        Document doc = Document.create();
        if (clue.getNumber() != null) {
            doc.put("number", clue.getNumber());
        }
        putIfHasText(doc, "event_name", clue.getEventName());
        putIfHasText(doc, "class_report_1", clue.getClassReport1());
        putIfHasText(doc, "class_report_2", clue.getClassReport2());
        if (clue.getClassReportList() != null && !clue.getClassReportList().isEmpty()) {
            doc.put("class_report_list", clue.getClassReportList());
        }
        putIfHasText(doc, "class_human_1", clue.getClassHuman1());
        putIfHasText(doc, "class_human_2", clue.getClassHuman2());
        if (clue.getClassHumanList() != null && !clue.getClassHumanList().isEmpty()) {
            doc.put("class_human_list", clue.getClassHumanList());
        }
        putIfHasText(doc, "products_components_services", clue.getProductsComponentsServices());
        putIfHasText(doc, "operating_entity", clue.getOperatingEntity());
        putIfHasText(doc, "operating_entity_human", clue.getOperatingEntityHuman());
        putIfHasText(doc, "risk_description", clue.getRiskDescription());
        putIfHasText(doc, "risk_description_human", clue.getRiskDescriptionHuman());
        putIfHasText(doc, "source_url", clue.getSourceUrl());
        putIfHasText(doc, "source_website", clue.getSourceWebsite());
        putIfHasText(doc, "paper_title", clue.getPaperTitle());
        putIfHasText(doc, "research_team", clue.getResearchTeam());
        putIfHasText(doc, "content", clue.getContent());
        putIfHasText(doc, "submit_user_name", clue.getSubmitUserName());
        putIfHasText(doc, "submission_channel", clue.getSubmissionChannel());
        putDateTime(doc, "submission_time", clue.getSubmissionTime());
        putIfHasText(doc, "submit_org_name", clue.getSubmitOrgName());
        if (clue.getIsSubmit() != null) {
            doc.put("is_submit", clue.getIsSubmit());
        }
        if (clue.getAuditStatus() != null) {
            doc.put("audit_status", clue.getAuditStatus());
        }
        if (clue.getIsWarehouse() != null) {
            doc.put("is_warehouse", clue.getIsWarehouse());
        }
        putDateTime(doc, "warehouse_time", clue.getWarehouseTime());
        putIfHasText(doc, "audit_reason", clue.getAuditReason());
        putIfHasText(doc, "audit_user_name", clue.getAuditUserName());
        putIfHasText(doc, "audit_dept_name", clue.getAuditDeptName());
        putDateTime(doc, "audit_time", clue.getAuditTime());
        putDateTime(doc, "create_time", clue.getCreateTime());
        putDateTime(doc, "update_time", clue.getUpdateTime());
        if (clue.getDeleted() != null) {
            doc.put("deleted", clue.getDeleted());
        }
        if (clue.getIsVerify() != null) {
            doc.put("is_verify", clue.getIsVerify());
        }
        if (clue.getIsShared() != null) {
            doc.put("is_shared", clue.getIsShared());
        }
        putDateTime(doc, "share_time", clue.getShareTime());
        return doc;
    }

    private void putIfHasText(Document doc, String field, String value) {
        if (hasText(value)) {
            doc.put(field, value.trim());
        }
    }

    private void putOptionalText(Document doc, String field, String value) {
        if (hasText(value)) {
            doc.put(field, value.trim());
        } else {
            doc.put(field, null);
        }
    }

    private void putDateTime(Document doc, String field, LocalDateTime value) {
        if (value != null) {
            doc.put(field, value.format(ES_DATE_TIME));
        }
    }

    @Override
    public void deleteById(String id) {
        if (!hasText(id)) {
            return;
        }
        BizRiskClue clue = getById(id.trim());
        if (clue == null) {
            throw new com.aisafe.common.exception.BusinessException("线索不存在");
        }
        if (clue.getAuditStatus() != null && clue.getAuditStatus() == 20) {
            throw new com.aisafe.common.exception.BusinessException("已审核的数据不可删除");
        }
        auditLogService.recordOperSuccess(
                "删除风险线索",
                BusinessType.DELETE,
                "RiskClueServiceImpl.deleteById",
                auditLogService.buildClueSnapshot(
                        clue.getId(),
                        clue.getTitle() != null ? clue.getTitle() : clue.getRiskDescription(),
                        clue.getAuditStatus()));
        bizRiskClueRepository.deleteById(id.trim());
    }

    @Override
    public Map<String, Object> toggleEventShare(String id) {
        if (!hasText(id)) {
            throw new com.aisafe.common.exception.BusinessException("事件ID不能为空");
        }
        BizRiskClue clue = getById(id.trim());
        if (clue == null) {
            throw new com.aisafe.common.exception.BusinessException("事件不存在");
        }
        if (clue.getAuditStatus() == null || clue.getAuditStatus() != 20
                || clue.getIsWarehouse() == null || clue.getIsWarehouse() != 1) {
            throw new com.aisafe.common.exception.BusinessException("仅已入库的安全事件可共享");
        }

        boolean currentlyShared = clue.getIsShared() != null && clue.getIsShared() == 1;
        LocalDateTime now = LocalDateTime.now();
        String nowStr = now.format(ES_DATE_TIME);
        Map<String, Object> result = new HashMap<>();

        if (currentlyShared) {
            UpdateQuery updateQuery = UpdateQuery.builder(id.trim())
                    .withScriptType(ScriptType.INLINE)
                    .withScript(
                            "ctx._source.is_shared = 0; "
                                    + "ctx._source.remove('share_time'); "
                                    + "ctx._source.update_time = params.now;")
                    .withParams(Map.of("now", nowStr))
                    .build();
            elasticsearchOperations.update(updateQuery,
                    elasticsearchOperations.getIndexCoordinatesFor(BizRiskClue.class));
            result.put("isShared", 0);
            result.put("shareTime", null);
        } else {
            Document doc = Document.create();
            doc.put("is_shared", 1);
            doc.put("share_time", nowStr);
            doc.put("update_time", nowStr);
            UpdateQuery updateQuery = UpdateQuery.builder(id.trim())
                    .withDocument(doc)
                    .build();
            elasticsearchOperations.update(updateQuery,
                    elasticsearchOperations.getIndexCoordinatesFor(BizRiskClue.class));
            result.put("isShared", 1);
            result.put("shareTime", nowStr);
        }
        return result;
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
