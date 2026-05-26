package com.aisafe.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("sys_oper_log")
public class SysOperLog {

    @TableId
    private Long id;
    private String title;
    private Integer businessType;
    private String method;
    private String requestMethod;
    private String operName;
    private String deptName;
    private String operUrl;
    private String operIp;
    private String operLocation;
    private String operParam;
    private String jsonResult;
    private Integer status;
    private String errorMsg;
    private LocalDateTime operTime;
    private Long costTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Integer getBusinessType() { return businessType; }
    public void setBusinessType(Integer businessType) { this.businessType = businessType; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getRequestMethod() { return requestMethod; }
    public void setRequestMethod(String requestMethod) { this.requestMethod = requestMethod; }
    public String getOperName() { return operName; }
    public void setOperName(String operName) { this.operName = operName; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public String getOperUrl() { return operUrl; }
    public void setOperUrl(String operUrl) { this.operUrl = operUrl; }
    public String getOperIp() { return operIp; }
    public void setOperIp(String operIp) { this.operIp = operIp; }
    public String getOperLocation() { return operLocation; }
    public void setOperLocation(String operLocation) { this.operLocation = operLocation; }
    public String getOperParam() { return operParam; }
    public void setOperParam(String operParam) { this.operParam = operParam; }
    public String getJsonResult() { return jsonResult; }
    public void setJsonResult(String jsonResult) { this.jsonResult = jsonResult; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
    public LocalDateTime getOperTime() { return operTime; }
    public void setOperTime(LocalDateTime operTime) { this.operTime = operTime; }
    public Long getCostTime() { return costTime; }
    public void setCostTime(Long costTime) { this.costTime = costTime; }
}
