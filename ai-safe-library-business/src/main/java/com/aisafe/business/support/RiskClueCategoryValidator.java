package com.aisafe.business.support;

import com.aisafe.business.dto.CategoryResolveResult;
import com.aisafe.business.dto.RiskReportExcelRow;
import com.aisafe.business.support.RiskReportImportError;
import com.aisafe.system.entity.BizTagCategory;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 解析/校验 Excel 一级/二级分类与 risk_clue 标签树的对应关系。
 * <ul>
 *   <li>一级必填；二级可选，为空且一级有效时仅保留一级</li>
 *   <li>一级、二级均有效且父子关系正确 → 原样导入</li>
 *   <li>一级有效、二级也有效，但二者不匹配 → 以二级为准，自动补全正确的一级</li>
 *   <li>一级无效、二级有效 → 以二级为准，自动补全正确的一级</li>
 *   <li>二级无效，或二级对应多个一级无法唯一确定 → 导入失败并提示</li>
 * </ul>
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

    public CategoryResolveResult resolve(RiskReportExcelRow row) {
        int rowNum = row.getRowNum();
        String l1Input = trim(row.getClassReport1());
        String l2Input = trim(row.getClassReport2());

        if (!StringUtils.hasText(l1Input)) {
            return CategoryResolveResult.fail(RiskReportImportError.of("CATEGORY_L1_REQUIRED",
                    "第 " + rowNum + " 行：一级分类不能为空"));
        }

        List<BizTagCategory> l1Roots = findRootTags(normalize(l1Input));
        boolean l1Valid = !l1Roots.isEmpty();

        if (!StringUtils.hasText(l2Input)) {
            if (l1Valid) {
                return CategoryResolveResult.ok(l1Roots.get(0).getTagName(), null);
            }
            return CategoryResolveResult.fail(RiskReportImportError.of("CATEGORY_L1_INVALID",
                    "第 " + rowNum + " 行：一级分类「" + l1Input + "」不在风险线索标签中"));
        }

        List<BizTagCategory> l2Tags = findLevel2Tags(normalize(l2Input));
        if (l2Tags.isEmpty()) {
            return CategoryResolveResult.fail(RiskReportImportError.of("CATEGORY_L2_INVALID",
                    "第 " + rowNum + " 行：二级分类「" + l2Input + "」不在风险线索标签中"));
        }

        CategoryResolveResult matched = matchParentChild(l1Roots, l2Tags);
        if (matched != null) {
            return matched;
        }

        // 一级/二级单独看都可能有效，但彼此不是父子关系（或一级无效）→ 以二级为准补全一级
        return resolveFromLevel2(rowNum, l2Input, l2Tags);
    }

    private CategoryResolveResult matchParentChild(List<BizTagCategory> l1Roots, List<BizTagCategory> l2Tags) {
        if (l1Roots.isEmpty()) {
            return null;
        }
        for (BizTagCategory l1Tag : l1Roots) {
            for (BizTagCategory l2Tag : l2Tags) {
                if (l1Tag.getId().equals(l2Tag.getParentId())) {
                    return CategoryResolveResult.ok(l1Tag.getTagName(), l2Tag.getTagName());
                }
            }
        }
        return null;
    }

    private CategoryResolveResult resolveFromLevel2(int rowNum, String l2Input, List<BizTagCategory> l2Tags) {
        Set<Long> parentIds = new HashSet<>();
        for (BizTagCategory l2Tag : l2Tags) {
            if (l2Tag.getParentId() != null && l2Tag.getParentId() != 0L) {
                parentIds.add(l2Tag.getParentId());
            }
        }
        if (parentIds.isEmpty()) {
            return CategoryResolveResult.fail(RiskReportImportError.of("CATEGORY_L2_INVALID",
                    "第 " + rowNum + " 行：二级分类「" + l2Input + "」不在风险线索标签中"));
        }
        if (parentIds.size() > 1) {
            return CategoryResolveResult.fail(RiskReportImportError.of("CATEGORY_L2_AMBIGUOUS",
                    "第 " + rowNum + " 行：二级分类「" + l2Input + "」对应多个一级分类，请填写正确的一级分类"));
        }

        BizTagCategory parent = byId.get(parentIds.iterator().next());
        if (parent == null) {
            return CategoryResolveResult.fail(RiskReportImportError.of("CATEGORY_L2_INVALID",
                    "第 " + rowNum + " 行：二级分类「" + l2Input + "」不在风险线索标签中"));
        }

        BizTagCategory matchedL2 = l2Tags.stream()
                .filter(tag -> parent.getId().equals(tag.getParentId()))
                .findFirst()
                .orElse(l2Tags.get(0));
        return CategoryResolveResult.ok(parent.getTagName(), matchedL2.getTagName());
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

    private List<BizTagCategory> findLevel2Tags(String name) {
        List<BizTagCategory> candidates = byName.get(name);
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        List<BizTagCategory> level2 = new ArrayList<>();
        for (BizTagCategory tag : candidates) {
            if (tag.getParentId() == null || tag.getParentId() == 0L) {
                continue;
            }
            BizTagCategory parent = byId.get(tag.getParentId());
            if (parent != null && isRootTag(parent)) {
                level2.add(tag);
            }
        }
        return level2;
    }

    private List<BizTagCategory> findRootTags(String name) {
        List<BizTagCategory> candidates = byName.get(name);
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        List<BizTagCategory> roots = new ArrayList<>();
        for (BizTagCategory tag : candidates) {
            if (isRootTag(tag)) {
                roots.add(tag);
            }
        }
        return roots;
    }

    private static boolean isRootTag(BizTagCategory tag) {
        return tag.getParentId() == null || tag.getParentId() == 0L;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
