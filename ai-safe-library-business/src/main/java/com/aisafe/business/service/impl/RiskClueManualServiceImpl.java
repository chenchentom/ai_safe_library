package com.aisafe.business.service.impl;

import com.aisafe.business.document.BizRiskClue;
import com.aisafe.business.document.BizRiskReviewRecord;
import com.aisafe.business.dto.RiskClueManualCreateDTO;
import com.aisafe.business.service.RiskClueManualService;
import com.aisafe.business.service.RiskClueService;
import com.aisafe.common.exception.BusinessException;
import com.aisafe.system.entity.SysDept;
import com.aisafe.system.entity.SysUser;
import com.aisafe.system.service.ISysDeptService;
import com.aisafe.system.service.ISysUserService;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class RiskClueManualServiceImpl implements RiskClueManualService {

    private static final DateTimeFormatter ES_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RiskClueService riskClueService;
    private final ISysUserService userService;
    private final ISysDeptService deptService;
    private final ElasticsearchOperations elasticsearchOperations;

    public RiskClueManualServiceImpl(RiskClueService riskClueService,
                                     ISysUserService userService,
                                     ISysDeptService deptService,
                                     ElasticsearchOperations elasticsearchOperations) {
        this.riskClueService = riskClueService;
        this.userService = userService;
        this.deptService = deptService;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @Override
    public String createClue(RiskClueManualCreateDTO dto) {
        validateBase(dto);
        LocalDateTime now = LocalDateTime.now();
        SysUser user = resolveCurrentUser();
        BizRiskClue clue = buildBaseClue(dto, user, now);
        applyPendingClueDefaults(clue);
        return riskClueService.save(clue);
    }

    @Override
    public String createEvent(RiskClueManualCreateDTO dto) {
        validateBase(dto);
        LocalDateTime now = LocalDateTime.now();
        SysUser user = resolveCurrentUser();
        BizRiskClue clue = buildBaseClue(dto, user, now);
        applyWarehousedEventDefaults(clue, now);
        syncHumanFieldsFromReport(clue);
        String id = riskClueService.save(clue);
        saveReviewRecord(id, clue, user, now);
        return id;
    }

    private BizRiskClue buildBaseClue(RiskClueManualCreateDTO dto, SysUser user, LocalDateTime now) {
        BizRiskClue clue = new BizRiskClue();
        clue.setEventName(dto.getEventName().trim());
        clue.setRiskDescription(dto.getRiskDescription().trim());
        clue.setContent(trimToNull(dto.getContent()));
        clue.setSourceUrl(trimToNull(dto.getSourceUrl()));
        clue.setSourceWebsite(trimToNull(dto.getSourceWebsite()));
        clue.setPaperTitle(trimToNull(dto.getPaperTitle()));
        clue.setResearchTeam(trimToNull(dto.getResearchTeam()));
        clue.setSubmissionChannel(trimToNull(dto.getSubmissionChannel()));
        clue.setOperatingEntity(trimToNull(dto.getOperatingEntity()));
        clue.setProductsComponentsServices(trimToNull(dto.getProductsComponentsServices()));
        applyAutoReportMeta(clue, user, now);
        clue.setDeleted(0);
        clue.setCreateTime(now);
        clue.setUpdateTime(now);
        applyReportCategory(clue, dto.getRiskCategory());
        return clue;
    }

    /** 安全事件：已审核、已入库，报送时间与审核时间一致 */
    private void applyWarehousedEventDefaults(BizRiskClue clue, LocalDateTime eventTime) {
        clue.setAuditStatus(20);
        clue.setIsWarehouse(1);
        clue.setSubmissionTime(eventTime);
        clue.setAuditTime(eventTime);
        clue.setWarehouseTime(eventTime);
        clue.setAuditReason(null);
    }

    /** 审核侧字段与报送侧保持一致 */
    private void syncHumanFieldsFromReport(BizRiskClue clue) {
        clue.setClassHuman1(clue.getClassReport1());
        clue.setClassHuman2(clue.getClassReport2());
        if (clue.getClassReportList() != null && !clue.getClassReportList().isEmpty()) {
            clue.setClassHumanList(new ArrayList<>(clue.getClassReportList()));
        } else if (hasText(clue.getClassReport1()) && hasText(clue.getClassReport2())) {
            clue.setClassHumanList(List.of(clue.getClassReport1() + "/" + clue.getClassReport2()));
        } else if (hasText(clue.getClassReport1())) {
            clue.setClassHumanList(List.of(clue.getClassReport1()));
        } else {
            clue.setClassHumanList(null);
        }
        clue.setRiskDescriptionHuman(clue.getRiskDescription());
        clue.setOperatingEntityHuman(clue.getOperatingEntity());
        clue.setAuditUserName(clue.getSubmitUserName());
        clue.setAuditDeptName(clue.getSubmitOrgName());
    }

    /** 待审核线索默认：未审核、未入库，清空审核侧字段 */
    private void applyPendingClueDefaults(BizRiskClue clue) {
        clue.setAuditStatus(10);
        clue.setIsWarehouse(0);
        clue.setWarehouseTime(null);
        clue.setAuditUserName(null);
        clue.setAuditDeptName(null);
        clue.setAuditTime(null);
        clue.setAuditReason(null);
        clue.setClassHuman1(null);
        clue.setClassHuman2(null);
        clue.setClassHumanList(null);
        clue.setRiskDescriptionHuman(null);
        clue.setOperatingEntityHuman(null);
    }

    private void applyAutoReportMeta(BizRiskClue clue, SysUser user, LocalDateTime now) {
        clue.setSubmissionTime(now);
        clue.setSubmitUserName(hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
        clue.setSubmitOrgName(resolveDeptName(user));
        clue.setIsSubmit(1);
    }

    private void saveReviewRecord(String clueId, BizRiskClue clue, SysUser user, LocalDateTime reviewTime) {
        String reviewerName = hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
        String deptName = resolveDeptName(user);
        String riskCategory = resolveRiskCategoryLabel(clue);

        Document doc = Document.create();
        doc.put("clue_id", clueId);
        doc.put("is_warehouse", 1);
        putIfHasText(doc, "class_human_1", clue.getClassHuman1());
        putIfHasText(doc, "class_human_2", clue.getClassHuman2());
        if (clue.getClassHumanList() != null && !clue.getClassHumanList().isEmpty()) {
            doc.put("class_human_list", clue.getClassHumanList());
        }
        putIfHasText(doc, "risk_category", riskCategory);
        putIfHasText(doc, "risk_description_human", clue.getRiskDescriptionHuman());
        putIfHasText(doc, "operating_entity_human", clue.getOperatingEntityHuman());
        doc.put("review_result", "reviewed");
        putIfHasText(doc, "reviewer", user.getUsername());
        putIfHasText(doc, "reviewer_name", reviewerName);
        putIfHasText(doc, "review_dept", deptName);
        doc.put("review_time", reviewTime.format(ES_DATE_TIME));
        doc.put("warehouse_time", reviewTime.format(ES_DATE_TIME));

        String recordId = UUID.randomUUID().toString();
        UpdateQuery indexQuery = UpdateQuery.builder(recordId)
                .withDocument(doc)
                .withUpsert(doc)
                .build();
        elasticsearchOperations.update(indexQuery,
                elasticsearchOperations.getIndexCoordinatesFor(BizRiskReviewRecord.class));
    }

    private String resolveRiskCategoryLabel(BizRiskClue clue) {
        if (clue.getClassHumanList() != null && !clue.getClassHumanList().isEmpty()) {
            return clue.getClassHumanList().get(0);
        }
        if (hasText(clue.getClassHuman1()) && hasText(clue.getClassHuman2())) {
            return clue.getClassHuman1() + "/" + clue.getClassHuman2();
        }
        return clue.getClassHuman1();
    }

    private void putIfHasText(Document doc, String field, String value) {
        if (hasText(value)) {
            doc.put(field, value.trim());
        }
    }

    private void validateBase(RiskClueManualCreateDTO dto) {
        if (dto == null) {
            throw new BusinessException("请求体不能为空");
        }
        if (!hasText(dto.getEventName())) {
            throw new BusinessException("请填写事件名称");
        }
        if (!hasText(dto.getRiskDescription())) {
            throw new BusinessException("请填写风险描述");
        }
    }

    private SysUser resolveCurrentUser() {
        long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    private String resolveDeptName(SysUser user) {
        if (user.getDeptId() == null) {
            return "";
        }
        SysDept dept = deptService.getById(user.getDeptId());
        return dept != null && dept.getDeptName() != null ? dept.getDeptName() : "";
    }

    private void applyReportCategory(BizRiskClue clue, String riskCategory) {
        if (!hasText(riskCategory)) {
            return;
        }
        String trimmed = riskCategory.trim();
        int slashIndex = trimmed.indexOf('/');
        if (slashIndex > 0) {
            String level1 = trimmed.substring(0, slashIndex).trim();
            String level2 = trimmed.substring(slashIndex + 1).trim();
            clue.setClassReport1(level1);
            clue.setClassReport2(level2);
            clue.setClassReportList(List.of(level1 + "/" + level2));
        } else {
            clue.setClassReport1(trimmed);
            clue.setClassReport2(null);
            clue.setClassReportList(List.of(trimmed));
        }
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
