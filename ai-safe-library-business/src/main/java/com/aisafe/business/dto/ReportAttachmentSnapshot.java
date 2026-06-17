package com.aisafe.business.dto;

/**
 * ES 线索文档中的报告附件快照（不含存储路径）
 */
public class ReportAttachmentSnapshot {

    private String id;
    private String fileName;
    private String contentType;
    private Long size;

    public ReportAttachmentSnapshot() {
    }

    public ReportAttachmentSnapshot(String id, String fileName, String contentType, Long size) {
        this.id = id;
        this.fileName = fileName;
        this.contentType = contentType;
        this.size = size;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }
}
