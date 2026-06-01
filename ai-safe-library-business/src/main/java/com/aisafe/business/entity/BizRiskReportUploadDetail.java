package com.aisafe.business.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("biz_risk_report_upload_detail")
public class BizRiskReportUploadDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long batchId;
    /** Excel 模板「序号」列 */
    private Integer serialNo;
    private String eventName;
    private String status;
    private String clueId;
    private String errorMessage;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }

    public Integer getSerialNo() { return serialNo; }
    public void setSerialNo(Integer serialNo) { this.serialNo = serialNo; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getClueId() { return clueId; }
    public void setClueId(String clueId) { this.clueId = clueId; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
