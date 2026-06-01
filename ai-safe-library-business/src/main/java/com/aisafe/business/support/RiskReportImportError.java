package com.aisafe.business.support;

/**
 * 导入行校验错误
 */
public class RiskReportImportError {

    private final String errorCode;
    private final String errorMessage;

    public RiskReportImportError(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }

    public static RiskReportImportError of(String code, String message) {
        return new RiskReportImportError(code, message);
    }
}
