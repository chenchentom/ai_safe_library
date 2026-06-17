package com.aisafe.business.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("biz_risk_report_upload_preview")
public class BizRiskReportUploadPreview {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String previewToken;
    private String excelPath;
    private String excelFileName;
    private String zipPath;
    private String zipFileName;
    private String extractedDir;
    private String matchResultJson;
    private Long submitUserId;
    private String submitOrgName;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPreviewToken() { return previewToken; }
    public void setPreviewToken(String previewToken) { this.previewToken = previewToken; }

    public String getExcelPath() { return excelPath; }
    public void setExcelPath(String excelPath) { this.excelPath = excelPath; }

    public String getExcelFileName() { return excelFileName; }
    public void setExcelFileName(String excelFileName) { this.excelFileName = excelFileName; }

    public String getZipPath() { return zipPath; }
    public void setZipPath(String zipPath) { this.zipPath = zipPath; }

    public String getZipFileName() { return zipFileName; }
    public void setZipFileName(String zipFileName) { this.zipFileName = zipFileName; }

    public String getExtractedDir() { return extractedDir; }
    public void setExtractedDir(String extractedDir) { this.extractedDir = extractedDir; }

    public String getMatchResultJson() { return matchResultJson; }
    public void setMatchResultJson(String matchResultJson) { this.matchResultJson = matchResultJson; }

    public Long getSubmitUserId() { return submitUserId; }
    public void setSubmitUserId(Long submitUserId) { this.submitUserId = submitUserId; }

    public String getSubmitOrgName() { return submitOrgName; }
    public void setSubmitOrgName(String submitOrgName) { this.submitOrgName = submitOrgName; }

    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
