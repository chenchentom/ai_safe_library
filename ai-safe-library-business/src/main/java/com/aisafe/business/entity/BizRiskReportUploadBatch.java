package com.aisafe.business.entity;

import com.aisafe.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("biz_risk_report_upload_batch")
public class BizRiskReportUploadBatch extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String batchNo;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private Integer totalCount;
    private Integer processedCount;
    private Integer successCount;
    private Integer failCount;
    private String status;
    private Long submitUserId;
    private String submitUserName;
    private String submitOrgName;
    private LocalDateTime submitTime;
    private LocalDateTime finishTime;
    private String errorSummary;
    private String zipFileName;
    private String zipFilePath;
    private String extractedDir;
    private Integer reportMatchedCount;
    private Integer reportMissingCount;
    private Integer reportOrphanCount;
    private String reportMatchSummary;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public Integer getTotalCount() { return totalCount; }
    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }

    public Integer getProcessedCount() { return processedCount; }
    public void setProcessedCount(Integer processedCount) { this.processedCount = processedCount; }

    public Integer getSuccessCount() { return successCount; }
    public void setSuccessCount(Integer successCount) { this.successCount = successCount; }

    public Integer getFailCount() { return failCount; }
    public void setFailCount(Integer failCount) { this.failCount = failCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getSubmitUserId() { return submitUserId; }
    public void setSubmitUserId(Long submitUserId) { this.submitUserId = submitUserId; }

    public String getSubmitUserName() { return submitUserName; }
    public void setSubmitUserName(String submitUserName) { this.submitUserName = submitUserName; }

    public String getSubmitOrgName() { return submitOrgName; }
    public void setSubmitOrgName(String submitOrgName) { this.submitOrgName = submitOrgName; }

    public LocalDateTime getSubmitTime() { return submitTime; }
    public void setSubmitTime(LocalDateTime submitTime) { this.submitTime = submitTime; }

    public LocalDateTime getFinishTime() { return finishTime; }
    public void setFinishTime(LocalDateTime finishTime) { this.finishTime = finishTime; }

    public String getErrorSummary() { return errorSummary; }
    public void setErrorSummary(String errorSummary) { this.errorSummary = errorSummary; }

    public String getZipFileName() { return zipFileName; }
    public void setZipFileName(String zipFileName) { this.zipFileName = zipFileName; }

    public String getZipFilePath() { return zipFilePath; }
    public void setZipFilePath(String zipFilePath) { this.zipFilePath = zipFilePath; }

    public String getExtractedDir() { return extractedDir; }
    public void setExtractedDir(String extractedDir) { this.extractedDir = extractedDir; }

    public Integer getReportMatchedCount() { return reportMatchedCount; }
    public void setReportMatchedCount(Integer reportMatchedCount) { this.reportMatchedCount = reportMatchedCount; }

    public Integer getReportMissingCount() { return reportMissingCount; }
    public void setReportMissingCount(Integer reportMissingCount) { this.reportMissingCount = reportMissingCount; }

    public Integer getReportOrphanCount() { return reportOrphanCount; }
    public void setReportOrphanCount(Integer reportOrphanCount) { this.reportOrphanCount = reportOrphanCount; }

    public String getReportMatchSummary() { return reportMatchSummary; }
    public void setReportMatchSummary(String reportMatchSummary) { this.reportMatchSummary = reportMatchSummary; }
}
