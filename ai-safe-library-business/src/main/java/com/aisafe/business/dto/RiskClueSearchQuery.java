package com.aisafe.business.dto;

/**
 * 风险线索搜索条件
 */
public class RiskClueSearchQuery {

    /** 关键词（事件名、内容） */
    private String keyword;

    /** 风险类别（报送类别，class_report_*） */
    private String riskCategory;

    /** 审核状态：10=未审核，20=已审核 */
    private Integer reviewStatus;

    /** 是否入库：0=否，1=是 */
    private Integer isWarehouse;

    /** 审核类别（class_human_*） */
    private String auditRiskCategory;

    /** 审核运营主体（模糊） */
    private String operatingEntityHuman;

    /** 审核人姓名（模糊） */
    private String auditUserName;

    /** 审核时间起 */
    private String auditStartTime;

    /** 审核时间止 */
    private String auditEndTime;

    /** 来源网站（模糊） */
    private String sourceWebsite;

    /** 运营主体（模糊） */
    private String operatingEntity;

    /** 报送渠道（模糊） */
    private String submissionChannel;

    /** 产品/组件/服务（模糊） */
    private String productsComponentsServices;

    /** 报送时间起 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss */
    private String submissionStartTime;

    /** 报送时间止 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss */
    private String submissionEndTime;

    /** 报送单位（数据隔离） */
    private String reportUnit;

    /** 兼容旧参数：来源类型精确匹配 submission_channel */
    private String sourceType;

    /** 排序字段，默认 submission_time */
    private String sortField = "submission_time";

    private int page = 1;
    private int size = 10;

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public String getRiskCategory() { return riskCategory; }
    public void setRiskCategory(String riskCategory) { this.riskCategory = riskCategory; }

    public Integer getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(Integer reviewStatus) { this.reviewStatus = reviewStatus; }

    public Integer getIsWarehouse() { return isWarehouse; }
    public void setIsWarehouse(Integer isWarehouse) { this.isWarehouse = isWarehouse; }

    public String getAuditRiskCategory() { return auditRiskCategory; }
    public void setAuditRiskCategory(String auditRiskCategory) { this.auditRiskCategory = auditRiskCategory; }

    public String getOperatingEntityHuman() { return operatingEntityHuman; }
    public void setOperatingEntityHuman(String operatingEntityHuman) {
        this.operatingEntityHuman = operatingEntityHuman;
    }

    public String getAuditUserName() { return auditUserName; }
    public void setAuditUserName(String auditUserName) { this.auditUserName = auditUserName; }

    public String getAuditStartTime() { return auditStartTime; }
    public void setAuditStartTime(String auditStartTime) { this.auditStartTime = auditStartTime; }

    public String getAuditEndTime() { return auditEndTime; }
    public void setAuditEndTime(String auditEndTime) { this.auditEndTime = auditEndTime; }

    public String getSourceWebsite() { return sourceWebsite; }
    public void setSourceWebsite(String sourceWebsite) { this.sourceWebsite = sourceWebsite; }

    public String getOperatingEntity() { return operatingEntity; }
    public void setOperatingEntity(String operatingEntity) { this.operatingEntity = operatingEntity; }

    public String getSubmissionChannel() { return submissionChannel; }
    public void setSubmissionChannel(String submissionChannel) { this.submissionChannel = submissionChannel; }

    public String getProductsComponentsServices() { return productsComponentsServices; }
    public void setProductsComponentsServices(String productsComponentsServices) {
        this.productsComponentsServices = productsComponentsServices;
    }

    public String getSubmissionStartTime() { return submissionStartTime; }
    public void setSubmissionStartTime(String submissionStartTime) { this.submissionStartTime = submissionStartTime; }

    public String getSubmissionEndTime() { return submissionEndTime; }
    public void setSubmissionEndTime(String submissionEndTime) { this.submissionEndTime = submissionEndTime; }

    public String getReportUnit() { return reportUnit; }
    public void setReportUnit(String reportUnit) { this.reportUnit = reportUnit; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getSortField() { return sortField; }
    public void setSortField(String sortField) { this.sortField = sortField; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}
