package com.aisafe.business.dto;

import com.aisafe.business.support.RiskReportImportError;

/**
 * Excel 报送分类解析结果（含自动纠正后的一级/二级分类）
 */
public class CategoryResolveResult {

    private final String level1;
    private final String level2;
    private final RiskReportImportError error;

    private CategoryResolveResult(String level1, String level2, RiskReportImportError error) {
        this.level1 = level1;
        this.level2 = level2;
        this.error = error;
    }

    public static CategoryResolveResult ok(String level1, String level2) {
        return new CategoryResolveResult(level1, level2, null);
    }

    public static CategoryResolveResult fail(RiskReportImportError error) {
        return new CategoryResolveResult(null, null, error);
    }

    public boolean isSuccess() {
        return error == null;
    }

    public String getLevel1() {
        return level1;
    }

    public String getLevel2() {
        return level2;
    }

    public RiskReportImportError getError() {
        return error;
    }
}
