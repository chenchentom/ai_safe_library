package com.aisafe.business.service.impl;

import com.aisafe.business.document.BizRiskClue;
import com.aisafe.business.document.BizRiskReviewRecord;
import com.aisafe.business.dto.BatchReviewResult;
import com.aisafe.business.dto.ReviewDTO;
import com.aisafe.business.dto.RiskClueSearchQuery;
import com.aisafe.business.repository.BizRiskClueRepository;
import com.aisafe.business.repository.BizRiskReviewRecordRepository;
import com.aisafe.business.service.RiskClueService;
import com.aisafe.business.service.RiskReviewService;
import com.aisafe.common.enums.BusinessType;
import com.aisafe.common.exception.BusinessException;
import com.aisafe.system.service.AuditLogService;
import com.aisafe.system.entity.SysDept;
import com.aisafe.system.entity.SysUser;
import com.aisafe.system.service.ISysDeptService;
import com.aisafe.system.service.ISysUserService;
import cn.dev33.satoken.stp.StpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 风险审核 Service 实现
 */
@Service
public class RiskReviewServiceImpl implements RiskReviewService {

    private static final Logger logger = LoggerFactory.getLogger(RiskReviewServiceImpl.class);

    private static final DateTimeFormatter ES_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final int BATCH_REVIEW_PAGE_SIZE = 100;
    private static final int MAX_FAILURE_DETAILS = 20;

    private final BizRiskClueRepository bizRiskClueRepository;
    private final BizRiskReviewRecordRepository bizRiskReviewRecordRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ISysUserService userService;
    private final ISysDeptService deptService;
    private final RiskClueService riskClueService;
    private final AuditLogService auditLogService;

    public RiskReviewServiceImpl(BizRiskClueRepository bizRiskClueRepository,
                                 BizRiskReviewRecordRepository bizRiskReviewRecordRepository,
                                 ElasticsearchOperations elasticsearchOperations,
                                 ISysUserService userService,
                                 ISysDeptService deptService,
                                 RiskClueService riskClueService,
                                 AuditLogService auditLogService) {
        this.bizRiskClueRepository = bizRiskClueRepository;
        this.bizRiskReviewRecordRepository = bizRiskReviewRecordRepository;
        this.elasticsearchOperations = elasticsearchOperations;
        this.userService = userService;
        this.deptService = deptService;
        this.riskClueService = riskClueService;
        this.auditLogService = auditLogService;
    }

    @Override
    public void review(ReviewDTO dto) {
        if (dto == null || dto.getClueId() == null || dto.getClueId().isBlank()) {
            throw new BusinessException("线索ID不能为空");
        }

        SysUser user = userService.getById(StpUtil.getLoginIdAsLong());
        String deptName = resolveDeptName(user);
        String reviewerName = hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
        LocalDateTime auditTime = LocalDateTime.now();

        BizRiskReviewRecord record = new BizRiskReviewRecord();
        record.setClueId(dto.getClueId());
        record.setIsWarehouse(normalizeWarehouse(dto.getIsWarehouse()));
        record.setRiskCategory(trimToNull(dto.getRiskCategory()));
        applyHumanCategoryToRecord(record, dto.getRiskCategory());
        record.setRiskDescriptionHuman(trimToNull(dto.getRiskDescriptionHuman()));
        record.setOperatingEntityHuman(trimToNull(dto.getOperatingEntityHuman()));
        record.setReviewComment(trimToNull(dto.getReviewComment()));
        record.setReviewer(user.getUsername());
        record.setReviewerName(reviewerName);
        record.setReviewDept(deptName);
        record.setReviewTime(auditTime);
        record.setReviewResult("reviewed");
        if (normalizeWarehouse(dto.getIsWarehouse()) == 1) {
            record.setWarehouseTime(auditTime);
        } else {
            record.setWarehouseTime(null);
        }

        updateClueAfterReview(dto.getClueId(), dto, reviewerName, deptName, auditTime);
        saveReviewRecord(record);

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("clueId", dto.getClueId());
        snapshot.put("isWarehouse", normalizeWarehouse(dto.getIsWarehouse()));
        snapshot.put("riskCategory", trimToNull(dto.getRiskCategory()));
        auditLogService.recordOperSuccess(
                "审核风险线索", BusinessType.REVIEW, "RiskReviewServiceImpl.review", snapshot);
        logger.info("审核完成 clueId={} reviewer={}", dto.getClueId(), reviewerName);
    }

    @Override
    @SuppressWarnings("unchecked")
    public BatchReviewResult batchReviewByQuery(RiskClueSearchQuery query, int batchIsWarehouse) {
        RiskClueSearchQuery searchQuery = query != null ? query : new RiskClueSearchQuery();
        searchQuery.setReviewStatus(10);
        searchQuery.setPage(1);
        searchQuery.setSize(BATCH_REVIEW_PAGE_SIZE);

        int warehouseDecision = batchIsWarehouse != 0 ? 1 : 0;

        Map<String, Object> firstPage = riskClueService.search(searchQuery);
        long totalPending = firstPage.get("total") instanceof Number number ? number.longValue() : 0L;

        BatchReviewResult result = new BatchReviewResult();
        result.setTotal((int) Math.min(totalPending, Integer.MAX_VALUE));

        if (totalPending <= 0) {
            return result;
        }

        int success = 0;
        int failed = 0;
        List<BatchReviewResult.FailureItem> failures = new ArrayList<>();
        Set<String> processedIds = new HashSet<>();

        while (true) {
            Map<String, Object> pageResult = riskClueService.search(searchQuery);
            List<BizRiskClue> rows = extractClueRows(pageResult);
            if (rows.isEmpty()) {
                break;
            }

            List<BizRiskClue> pendingRows = new ArrayList<>();
            for (BizRiskClue clue : rows) {
                if (clue == null || !hasText(clue.getId())) {
                    continue;
                }
                if (processedIds.contains(clue.getId())) {
                    continue;
                }
                if (clue.getAuditStatus() != null && clue.getAuditStatus() != 10) {
                    processedIds.add(clue.getId());
                    continue;
                }
                pendingRows.add(clue);
            }

            // ES 近实时索引未刷新时，可能反复返回同一页；无新线索则结束，避免重复审核
            if (pendingRows.isEmpty()) {
                break;
            }

            for (BizRiskClue clue : pendingRows) {
                processedIds.add(clue.getId());
                ReviewDTO dto = buildAutoReviewFromClue(clue, warehouseDecision);
                String validationError = validateAutoReview(dto);
                if (validationError != null) {
                    failed++;
                    addFailure(failures, clue, validationError);
                    continue;
                }
                try {
                    review(dto);
                    success++;
                } catch (Exception ex) {
                    failed++;
                    String reason = ex.getMessage() != null ? ex.getMessage() : "审核失败";
                    addFailure(failures, clue, reason);
                    logger.warn("批量审核失败 clueId={} reason={}", clue.getId(), reason);
                }
            }
        }

        result.setSuccess(success);
        result.setFailed(failed);
        result.setFailures(failures);

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("total", result.getTotal());
        snapshot.put("success", success);
        snapshot.put("failed", failed);
        snapshot.put("isWarehouse", warehouseDecision);
        auditLogService.recordOperSuccess(
                "批量审核风险线索", BusinessType.REVIEW, "RiskReviewServiceImpl.batchReviewByQuery", snapshot);
        logger.info("批量审核完成 total={} success={} failed={}", result.getTotal(), success, failed);
        return result;
    }

    @Override
    public List<BizRiskReviewRecord> getReviewHistory(String clueId) {
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.term(t -> t.field("clue_id").value(clueId)))
                .withSort(Sort.by(Sort.Direction.DESC, "review_time"))
                .build();
        SearchHits<BizRiskReviewRecord> hits = elasticsearchOperations.search(
                query,
                BizRiskReviewRecord.class,
                elasticsearchOperations.getIndexCoordinatesFor(BizRiskReviewRecord.class)
        );
        return hits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }

    private void updateClueAfterReview(String clueId, ReviewDTO dto, String reviewerName,
                                     String deptName, LocalDateTime auditTime) {
        if (bizRiskClueRepository.findById(clueId).isEmpty()) {
            throw new BusinessException("线索不存在");
        }

        Document doc = Document.create();
        doc.put("audit_status", 20);
        int isWarehouse = normalizeWarehouse(dto.getIsWarehouse());
        doc.put("is_warehouse", isWarehouse);
        applyWarehouseTime(doc, isWarehouse, auditTime);
        applyHumanCategory(doc, dto.getRiskCategory());
        putIfHasText(doc, "risk_description_human", dto.getRiskDescriptionHuman());
        putIfHasText(doc, "operating_entity_human", dto.getOperatingEntityHuman());
        doc.put("audit_user_name", reviewerName);
        putIfHasText(doc, "audit_dept_name", deptName);
        doc.put("audit_time", auditTime.format(ES_DATE_TIME));
        putIfHasText(doc, "audit_reason", dto.getReviewComment());
        doc.put("update_time", auditTime.format(ES_DATE_TIME));

        UpdateQuery updateQuery = UpdateQuery.builder(clueId)
                .withDocument(doc)
                .build();
        elasticsearchOperations.update(updateQuery, elasticsearchOperations.getIndexCoordinatesFor(BizRiskClue.class));
    }

    private void saveReviewRecord(BizRiskReviewRecord record) {
        Document doc = Document.create();
        doc.put("clue_id", record.getClueId());
        doc.put("is_warehouse", record.getIsWarehouse());
        putIfHasText(doc, "class_human_1", record.getClassHuman1());
        putIfHasText(doc, "class_human_2", record.getClassHuman2());
        if (record.getClassHumanList() != null && !record.getClassHumanList().isEmpty()) {
            doc.put("class_human_list", record.getClassHumanList());
        }
        putIfHasText(doc, "risk_category", record.getRiskCategory());
        putIfHasText(doc, "risk_description_human", record.getRiskDescriptionHuman());
        putIfHasText(doc, "operating_entity_human", record.getOperatingEntityHuman());
        putIfHasText(doc, "review_result", record.getReviewResult());
        putIfHasText(doc, "review_comment", record.getReviewComment());
        putIfHasText(doc, "reviewer", record.getReviewer());
        putIfHasText(doc, "reviewer_name", record.getReviewerName());
        putIfHasText(doc, "review_dept", record.getReviewDept());
        doc.put("review_time", record.getReviewTime().format(ES_DATE_TIME));
        if (record.getIsWarehouse() != null && record.getIsWarehouse() == 1 && record.getWarehouseTime() != null) {
            doc.put("warehouse_time", record.getWarehouseTime().format(ES_DATE_TIME));
        } else {
            doc.put("warehouse_time", null);
        }

        String recordId = UUID.randomUUID().toString();
        UpdateQuery indexQuery = UpdateQuery.builder(recordId)
                .withDocument(doc)
                .withUpsert(doc)
                .build();
        elasticsearchOperations.update(indexQuery,
                elasticsearchOperations.getIndexCoordinatesFor(BizRiskReviewRecord.class));
        record.setId(recordId);
    }

    private void applyHumanCategory(Document doc, String riskCategory) {
        if (!hasText(riskCategory)) {
            return;
        }
        String trimmed = riskCategory.trim();
        int slashIndex = trimmed.indexOf('/');
        if (slashIndex > 0) {
            String level1 = trimmed.substring(0, slashIndex).trim();
            String level2 = trimmed.substring(slashIndex + 1).trim();
            doc.put("class_human_1", level1);
            doc.put("class_human_2", level2);
            doc.put("class_human_list", List.of(level1 + "/" + level2));
        } else {
            doc.put("class_human_1", trimmed);
            doc.put("class_human_2", null);
            doc.put("class_human_list", List.of(trimmed));
        }
    }

    private void applyHumanCategoryToRecord(BizRiskReviewRecord record, String riskCategory) {
        if (!hasText(riskCategory)) {
            return;
        }
        String trimmed = riskCategory.trim();
        int slashIndex = trimmed.indexOf('/');
        if (slashIndex > 0) {
            String level1 = trimmed.substring(0, slashIndex).trim();
            String level2 = trimmed.substring(slashIndex + 1).trim();
            record.setClassHuman1(level1);
            record.setClassHuman2(level2);
            record.setClassHumanList(List.of(level1 + "/" + level2));
        } else {
            record.setClassHuman1(trimmed);
            record.setClassHuman2(null);
            record.setClassHumanList(List.of(trimmed));
        }
    }

    private void putIfHasText(Document doc, String field, String value) {
        if (hasText(value)) {
            doc.put(field, value.trim());
        }
    }

    /** 入库时写入入库时间，未入库时清空该字段 */
    private void applyWarehouseTime(Document doc, int isWarehouse, LocalDateTime warehouseTime) {
        if (isWarehouse == 1) {
            doc.put("warehouse_time", warehouseTime.format(ES_DATE_TIME));
        } else {
            doc.put("warehouse_time", null);
        }
    }

    private String resolveDeptName(SysUser user) {
        if (user.getDeptId() == null) {
            return "";
        }
        SysDept dept = deptService.getById(user.getDeptId());
        return dept != null && dept.getDeptName() != null ? dept.getDeptName() : "";
    }

    private Integer normalizeWarehouse(Integer isWarehouse) {
        if (isWarehouse == null) {
            return 0;
        }
        return isWarehouse != 0 ? 1 : 0;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim();
    }

    @SuppressWarnings("unchecked")
    private List<BizRiskClue> extractClueRows(Map<String, Object> pageResult) {
        if (pageResult == null || pageResult.get("rows") == null) {
            return List.of();
        }
        Object rows = pageResult.get("rows");
        if (rows instanceof List<?> list) {
            List<BizRiskClue> clues = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof BizRiskClue clue) {
                    clues.add(clue);
                }
            }
            return clues;
        }
        return List.of();
    }

    private ReviewDTO buildAutoReviewFromClue(BizRiskClue clue, int batchIsWarehouse) {
        ReviewDTO dto = new ReviewDTO();
        dto.setClueId(clue.getId());
        dto.setIsWarehouse(batchIsWarehouse != 0 ? 1 : 0);
        dto.setRiskCategory(resolveReportCategory(clue));
        dto.setRiskDescriptionHuman(resolveRiskDescription(clue));
        dto.setOperatingEntityHuman(trimToNull(clue.getOperatingEntity()));
        return dto;
    }

    private String validateAutoReview(ReviewDTO dto) {
        if (!hasText(dto.getRiskCategory())) {
            return "缺少报送风险类别";
        }
        if (!hasText(dto.getRiskDescriptionHuman())) {
            return "缺少报送风险描述";
        }
        return null;
    }

    private String resolveReportCategory(BizRiskClue clue) {
        String level1 = trimToNull(clue.getClassReport1());
        String level2 = trimToNull(clue.getClassReport2());
        if (hasText(level1) && hasText(level2)) {
            return level1 + "/" + level2;
        }
        if (hasText(level1)) {
            return level1;
        }
        if (clue.getClassReportList() != null) {
            for (String item : clue.getClassReportList()) {
                if (hasText(item)) {
                    return item.trim();
                }
            }
        }
        return null;
    }

    private String resolveRiskDescription(BizRiskClue clue) {
        if (hasText(clue.getRiskDescription())) {
            return clue.getRiskDescription().trim();
        }
        if (hasText(clue.getContent())) {
            return clue.getContent().trim();
        }
        return null;
    }

    private void addFailure(List<BatchReviewResult.FailureItem> failures, BizRiskClue clue, String reason) {
        if (failures.size() >= MAX_FAILURE_DETAILS) {
            return;
        }
        String eventName = hasText(clue.getEventName()) ? clue.getEventName().trim() : clue.getId();
        failures.add(new BatchReviewResult.FailureItem(clue.getId(), eventName, reason));
    }
}
