package com.aisafe.business.service.impl;

import com.aisafe.business.document.BizRiskClue;
import com.aisafe.business.document.BizRiskReviewRecord;
import com.aisafe.business.dto.ReviewDTO;
import com.aisafe.business.repository.BizRiskClueRepository;
import com.aisafe.business.repository.BizRiskReviewRecordRepository;
import com.aisafe.business.service.RiskReviewService;
import com.aisafe.common.exception.BusinessException;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 风险审核 Service 实现
 */
@Service
public class RiskReviewServiceImpl implements RiskReviewService {

    private static final Logger logger = LoggerFactory.getLogger(RiskReviewServiceImpl.class);

    private static final DateTimeFormatter ES_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BizRiskClueRepository bizRiskClueRepository;
    private final BizRiskReviewRecordRepository bizRiskReviewRecordRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ISysUserService userService;
    private final ISysDeptService deptService;

    public RiskReviewServiceImpl(BizRiskClueRepository bizRiskClueRepository,
                                 BizRiskReviewRecordRepository bizRiskReviewRecordRepository,
                                 ElasticsearchOperations elasticsearchOperations,
                                 ISysUserService userService,
                                 ISysDeptService deptService) {
        this.bizRiskClueRepository = bizRiskClueRepository;
        this.bizRiskReviewRecordRepository = bizRiskReviewRecordRepository;
        this.elasticsearchOperations = elasticsearchOperations;
        this.userService = userService;
        this.deptService = deptService;
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
        logger.info("审核完成 clueId={} reviewer={}", dto.getClueId(), reviewerName);
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
}
