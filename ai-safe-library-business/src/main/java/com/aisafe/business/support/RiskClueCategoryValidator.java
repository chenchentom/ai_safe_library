package com.aisafe.business.support;

import com.aisafe.business.dto.RiskReportExcelRow;
import com.aisafe.system.entity.BizTagCategory;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 校验 Excel 一级/二级分类是否在 risk_clue 标签树中存在且为合法父子关系
 */
public class RiskClueCategoryValidator {

    private final Map<Long, BizTagCategory> byId = new HashMap<>();
    private final Map<String, List<BizTagCategory>> byName = new HashMap<>();

    public RiskClueCategoryValidator(List<BizTagCategory> tags) {
        if (tags == null) {
            return;
        }
        for (BizTagCategory tag : tags) {
            if (tag == null || !"risk_clue".equals(tag.getModule())) {
                continue;
            }
            if (!"0".equals(tag.getStatus())) {
                continue;
            }
            byId.put(tag.getId(), tag);
            byName.computeIfAbsent(normalize(tag.getTagName()), k -> new ArrayList<>()).add(tag);
        }
    }

    public RiskReportImportError validate(RiskReportExcelRow row) {
        int rowNum = row.getRowNum();
        String l1 = trim(row.getClassReport1());
        String l2 = trim(row.getClassReport2());

        if (!StringUtils.hasText(l1)) {
            return RiskReportImportError.of("CATEGORY_L1_REQUIRED",
                    "第 " + rowNum + " 行：一级分类不能为空");
        }
        if (!StringUtils.hasText(l2)) {
            return RiskReportImportError.of("CATEGORY_L2_REQUIRED",
                    "第 " + rowNum + " 行：二级分类不能为空");
        }

        List<BizTagCategory> level1Candidates = byName.get(normalize(l1));
        if (level1Candidates == null || level1Candidates.isEmpty()) {
            return RiskReportImportError.of("CATEGORY_L1_INVALID",
                    "第 " + rowNum + " 行：一级分类「" + l1 + "」不在风险线索标签中");
        }

        for (BizTagCategory level1 : level1Candidates) {
            List<BizTagCategory> level2Candidates = byName.get(normalize(l2));
            if (level2Candidates == null) {
                continue;
            }
            for (BizTagCategory level2 : level2Candidates) {
                if (level1.getId().equals(level2.getParentId())) {
                    return null;
                }
            }
        }

        return RiskReportImportError.of("CATEGORY_L2_INVALID",
                "第 " + rowNum + " 行：二级分类「" + l2 + "」与一级分类「" + l1 + "」不匹配");
    }

    public RiskReportImportError validateRequiredFields(RiskReportExcelRow row) {
        int rowNum = row.getRowNum();
        if (!StringUtils.hasText(trim(row.getEventName()))) {
            return RiskReportImportError.of("EVENT_NAME_REQUIRED",
                    "第 " + rowNum + " 行：事件名不能为空");
        }
        if (!StringUtils.hasText(trim(row.getRiskDescription()))) {
            return RiskReportImportError.of("RISK_DESC_REQUIRED",
                    "第 " + rowNum + " 行：风险描述不能为空");
        }
        return null;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
