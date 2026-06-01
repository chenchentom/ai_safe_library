package com.aisafe.business.support;

import com.aisafe.business.document.BizRiskClue;
import com.aisafe.business.dto.RiskReportExcelRow;
import com.aisafe.system.entity.SysDept;
import com.aisafe.system.entity.SysUser;
import com.aisafe.system.service.ISysDeptService;
import com.aisafe.system.service.ISysUserService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 将 Excel 行构建为待审核线索（与手动新增逻辑对齐）
 */
@Component
public class RiskReportClueBuilder {

    private final ISysUserService userService;
    private final ISysDeptService deptService;

    public RiskReportClueBuilder(ISysUserService userService, ISysDeptService deptService) {
        this.userService = userService;
        this.deptService = deptService;
    }

    public BizRiskClue buildPendingClue(RiskReportExcelRow row, SysUser loginUser, LocalDateTime defaultTime) {
        LocalDateTime now = defaultTime != null ? defaultTime : LocalDateTime.now();
        BizRiskClue clue = new BizRiskClue();
        clue.setNumber(row.getNumber());
        clue.setEventName(trim(row.getEventName()));
        clue.setContent(trim(row.getContent()));
        clue.setProductsComponentsServices(trim(row.getProductsComponentsServices()));
        clue.setOperatingEntity(trim(row.getOperatingEntity()));
        clue.setRiskDescription(trim(row.getRiskDescription()));
        clue.setSourceUrl(trim(row.getSourceUrl()));
        clue.setSourceWebsite(trim(row.getSourceWebsite()));
        clue.setPaperTitle(trim(row.getPaperTitle()));
        clue.setResearchTeam(trim(row.getResearchTeam()));
        clue.setIsVerify(row.getIsVerify());
        clue.setIsSubmit(row.getIsSubmit() != null ? row.getIsSubmit() : 1);
        clue.setSubmissionChannel(trim(row.getSubmissionChannel()));

        applyReportCategory(clue, row.getClassReport1(), row.getClassReport2());
        applyReportMeta(clue, row, loginUser, now);
        applyPendingDefaults(clue);
        clue.setDeleted(0);
        clue.setCreateTime(now);
        clue.setUpdateTime(now);
        return clue;
    }

    public RiskReportImportError validateRowFields(RiskReportExcelRow row, RiskClueCategoryValidator categoryValidator) {
        RiskReportImportError required = categoryValidator.validateRequiredFields(row);
        if (required != null) {
            return required;
        }
        if (StringUtils.hasText(row.getRawIsVerifyText()) && row.getIsVerify() == null) {
            return RiskReportImportError.of("BOOL_FIELD_INVALID",
                    "第 " + row.getRowNum() + " 行：「是否验证」只能填 是/否");
        }
        if (StringUtils.hasText(row.getRawIsSubmitText()) && row.getIsSubmit() == null) {
            return RiskReportImportError.of("BOOL_FIELD_INVALID",
                    "第 " + row.getRowNum() + " 行：「是否报送」只能填 是/否");
        }
        if (StringUtils.hasText(row.getRawSubmissionTimeText()) && row.getSubmissionTime() == null) {
            return RiskReportImportError.of("SUBMISSION_TIME_INVALID",
                    "第 " + row.getRowNum() + " 行：报送时间无法解析");
        }
        return categoryValidator.validate(row);
    }

    private void applyReportCategory(BizRiskClue clue, String level1, String level2) {
        String l1 = trim(level1);
        String l2 = trim(level2);
        if (!StringUtils.hasText(l1)) {
            return;
        }
        clue.setClassReport1(l1);
        if (StringUtils.hasText(l2)) {
            clue.setClassReport2(l2);
            clue.setClassReportList(List.of(l1 + "/" + l2));
        } else {
            clue.setClassReport2(null);
            clue.setClassReportList(List.of(l1));
        }
    }

    private void applyReportMeta(BizRiskClue clue, RiskReportExcelRow row, SysUser loginUser, LocalDateTime now) {
        LocalDateTime submissionTime = row.getSubmissionTime() != null ? row.getSubmissionTime() : now;
        clue.setSubmissionTime(submissionTime);

        String submitUser = StringUtils.hasText(row.getSubmitUserName())
                ? row.getSubmitUserName().trim()
                : displayName(loginUser);
        clue.setSubmitUserName(submitUser);
        clue.setSubmitOrgName(resolveSubmitOrgName(submitUser, loginUser));
    }

    private void applyPendingDefaults(BizRiskClue clue) {
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
        clue.setIsShared(0);
        clue.setShareTime(null);
    }

    private String resolveSubmitOrgName(String submitUserName, SysUser loginUser) {
        if (!StringUtils.hasText(submitUserName)) {
            return resolveDeptName(loginUser);
        }
        String name = submitUserName.trim();
        SysUser submitter = userService.getByNickname(name);
        if (submitter == null) {
            submitter = userService.getByUsername(name);
        }
        if (submitter == null && name.equals(displayName(loginUser))) {
            submitter = loginUser;
        }
        if (submitter != null) {
            return resolveDeptName(submitter);
        }
        return resolveDeptName(loginUser);
    }

    private String displayName(SysUser user) {
        return StringUtils.hasText(user.getNickname()) ? user.getNickname().trim() : user.getUsername();
    }

    private String resolveDeptName(SysUser user) {
        if (user.getDeptId() == null) {
            return "";
        }
        SysDept dept = deptService.getById(user.getDeptId());
        return dept != null && dept.getDeptName() != null ? dept.getDeptName() : "";
    }

    private static String trim(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
