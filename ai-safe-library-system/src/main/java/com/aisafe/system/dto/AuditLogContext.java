package com.aisafe.system.dto;

/**
 * 异步写入前捕获的请求上下文
 */
public class AuditLogContext {

    private String operName;
    private String deptName;
    private String operIp;
    private String operUrl;
    private String requestMethod;
    private String userAgent;

    public String getOperName() { return operName; }
    public void setOperName(String operName) { this.operName = operName; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public String getOperIp() { return operIp; }
    public void setOperIp(String operIp) { this.operIp = operIp; }
    public String getOperUrl() { return operUrl; }
    public void setOperUrl(String operUrl) { this.operUrl = operUrl; }
    public String getRequestMethod() { return requestMethod; }
    public void setRequestMethod(String requestMethod) { this.requestMethod = requestMethod; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
}
