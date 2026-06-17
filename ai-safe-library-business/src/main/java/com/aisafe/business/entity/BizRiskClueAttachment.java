package com.aisafe.business.entity;

import com.aisafe.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("biz_risk_clue_attachment")
public class BizRiskClueAttachment extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String clueId;
    private Long batchId;
    private Integer serialNo;
    private String originalName;
    private String storedName;
    private String storagePath;
    private String contentType;
    private Long fileSize;
    private Integer sortOrder;
    private Long uploadUserId;
    private LocalDateTime uploadTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getClueId() { return clueId; }
    public void setClueId(String clueId) { this.clueId = clueId; }

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }

    public Integer getSerialNo() { return serialNo; }
    public void setSerialNo(Integer serialNo) { this.serialNo = serialNo; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public String getStoredName() { return storedName; }
    public void setStoredName(String storedName) { this.storedName = storedName; }

    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Long getUploadUserId() { return uploadUserId; }
    public void setUploadUserId(Long uploadUserId) { this.uploadUserId = uploadUserId; }

    public LocalDateTime getUploadTime() { return uploadTime; }
    public void setUploadTime(LocalDateTime uploadTime) { this.uploadTime = uploadTime; }
}
