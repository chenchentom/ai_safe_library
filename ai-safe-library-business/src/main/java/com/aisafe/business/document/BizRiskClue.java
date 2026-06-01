package com.aisafe.business.document;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;

@Document(indexName = "biz_risk_clue")
public class BizRiskClue {

    @Id
    private String id;

    // ==================== 新字段（下划线格式） ====================
    @Field(name = "number", type = FieldType.Integer)
    private Integer number;

    @Field(name = "event_name", type = FieldType.Text, analyzer = "standard", searchAnalyzer = "standard")
    private String eventName;

    @Field(name = "class_report_1", type = FieldType.Keyword)
    private String classReport1;

    @Field(name = "class_report_2", type = FieldType.Keyword)
    private String classReport2;

    @Field(name = "class_report_list", type = FieldType.Keyword)
    private List<String> classReportList;

    @Field(name = "class_human_1", type = FieldType.Keyword)
    private String classHuman1;

    @Field(name = "class_human_2", type = FieldType.Keyword)
    private String classHuman2;

    @Field(name = "class_human_list", type = FieldType.Keyword)
    private List<String> classHumanList;

    @Field(name = "products_components_services", type = FieldType.Text, analyzer = "standard")
    private String productsComponentsServices;

    @Field(name = "operating_entity", type = FieldType.Keyword)
    private String operatingEntity;

    @Field(name = "operating_entity_human", type = FieldType.Keyword)
    private String operatingEntityHuman;

    @Field(name = "risk_description", type = FieldType.Text, analyzer = "standard")
    private String riskDescription;

    @Field(name = "risk_description_human", type = FieldType.Text, analyzer = "standard")
    private String riskDescriptionHuman;

    @Field(name = "source_url", type = FieldType.Keyword, ignoreAbove = 2048)
    private String sourceUrl;

    @Field(name = "source_website", type = FieldType.Keyword)
    private String sourceWebsite;

    @Field(name = "paper_title", type = FieldType.Text, analyzer = "standard")
    private String paperTitle;

    @Field(name = "research_team", type = FieldType.Keyword)
    private String researchTeam;

    @Field(name = "content", type = FieldType.Text, analyzer = "standard")
    private String content;

    @Field(name = "submit_user_name", type = FieldType.Keyword)
    private String submitUserName;

    @Field(name = "submission_channel", type = FieldType.Keyword)
    private String submissionChannel;

    @Field(name = "submission_time", type = FieldType.Date, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime submissionTime;

    @Field(name = "submit_org_name", type = FieldType.Keyword)
    private String submitOrgName;

    @Field(name = "is_submit", type = FieldType.Integer)
    private Integer isSubmit;

    @Field(name = "audit_status", type = FieldType.Integer)
    private Integer auditStatus;

    @Field(name = "is_warehouse", type = FieldType.Integer)
    private Integer isWarehouse;

    @Field(name = "warehouse_time", type = FieldType.Date, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime warehouseTime;

    @Field(name = "audit_reason", type = FieldType.Text, analyzer = "standard")
    private String auditReason;

    @Field(name = "audit_user_name", type = FieldType.Keyword)
    private String auditUserName;

    @Field(name = "audit_dept_name", type = FieldType.Keyword)
    private String auditDeptName;

    @Field(name = "audit_time", type = FieldType.Date, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime auditTime;

    @Field(name = "create_time", type = FieldType.Date, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;

    @Field(name = "update_time", type = FieldType.Date, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updateTime;

    @Field(name = "deleted", type = FieldType.Integer)
    private Integer deleted;

    @Field(name = "is_verify", type = FieldType.Integer)
    private Integer isVerify;

    /** 是否共享：1 已共享 0 未共享 */
    @Field(name = "is_shared", type = FieldType.Integer)
    private Integer isShared;

    @Field(name = "share_time", type = FieldType.Date, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime shareTime;

    // ==================== Getters and Setters ====================
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Integer getNumber() { return number; }
    public void setNumber(Integer number) { this.number = number; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public String getClassReport1() { return classReport1; }
    public void setClassReport1(String classReport1) { this.classReport1 = classReport1; }

    public String getClassReport2() { return classReport2; }
    public void setClassReport2(String classReport2) { this.classReport2 = classReport2; }

    public List<String> getClassReportList() { return classReportList; }
    public void setClassReportList(List<String> classReportList) { this.classReportList = classReportList; }

    public String getClassHuman1() { return classHuman1; }
    public void setClassHuman1(String classHuman1) { this.classHuman1 = classHuman1; }

    public String getClassHuman2() { return classHuman2; }
    public void setClassHuman2(String classHuman2) { this.classHuman2 = classHuman2; }

    public List<String> getClassHumanList() { return classHumanList; }
    public void setClassHumanList(List<String> classHumanList) { this.classHumanList = classHumanList; }

    public String getProductsComponentsServices() { return productsComponentsServices; }
    public void setProductsComponentsServices(String productsComponentsServices) { this.productsComponentsServices = productsComponentsServices; }

    public String getOperatingEntity() { return operatingEntity; }
    public void setOperatingEntity(String operatingEntity) { this.operatingEntity = operatingEntity; }

    public String getOperatingEntityHuman() { return operatingEntityHuman; }
    public void setOperatingEntityHuman(String operatingEntityHuman) { this.operatingEntityHuman = operatingEntityHuman; }

    public String getRiskDescription() { return riskDescription; }
    public void setRiskDescription(String riskDescription) { this.riskDescription = riskDescription; }

    public String getRiskDescriptionHuman() { return riskDescriptionHuman; }
    public void setRiskDescriptionHuman(String riskDescriptionHuman) { this.riskDescriptionHuman = riskDescriptionHuman; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public String getSourceWebsite() { return sourceWebsite; }
    public void setSourceWebsite(String sourceWebsite) { this.sourceWebsite = sourceWebsite; }

    public String getPaperTitle() { return paperTitle; }
    public void setPaperTitle(String paperTitle) { this.paperTitle = paperTitle; }

    public String getResearchTeam() { return researchTeam; }
    public void setResearchTeam(String researchTeam) { this.researchTeam = researchTeam; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSubmitUserName() { return submitUserName; }
    public void setSubmitUserName(String submitUserName) { this.submitUserName = submitUserName; }

    public String getSubmissionChannel() { return submissionChannel; }
    public void setSubmissionChannel(String submissionChannel) { this.submissionChannel = submissionChannel; }

    public LocalDateTime getSubmissionTime() { return submissionTime; }
    public void setSubmissionTime(LocalDateTime submissionTime) { this.submissionTime = submissionTime; }

    public String getSubmitOrgName() { return submitOrgName; }
    public void setSubmitOrgName(String submitOrgName) { this.submitOrgName = submitOrgName; }

    public Integer getIsSubmit() { return isSubmit; }
    public void setIsSubmit(Integer isSubmit) { this.isSubmit = isSubmit; }

    public Integer getAuditStatus() { return auditStatus; }
    public void setAuditStatus(Integer auditStatus) { this.auditStatus = auditStatus; }

    public Integer getIsWarehouse() { return isWarehouse; }
    public void setIsWarehouse(Integer isWarehouse) { this.isWarehouse = isWarehouse; }

    public LocalDateTime getWarehouseTime() { return warehouseTime; }
    public void setWarehouseTime(LocalDateTime warehouseTime) { this.warehouseTime = warehouseTime; }

    public String getAuditReason() { return auditReason; }
    public void setAuditReason(String auditReason) { this.auditReason = auditReason; }

    public String getAuditUserName() { return auditUserName; }
    public void setAuditUserName(String auditUserName) { this.auditUserName = auditUserName; }

    public String getAuditDeptName() { return auditDeptName; }
    public void setAuditDeptName(String auditDeptName) { this.auditDeptName = auditDeptName; }

    public LocalDateTime getAuditTime() { return auditTime; }
    public void setAuditTime(LocalDateTime auditTime) { this.auditTime = auditTime; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }

    public Integer getIsVerify() { return isVerify; }
    public void setIsVerify(Integer isVerify) { this.isVerify = isVerify; }

    public Integer getIsShared() { return isShared; }
    public void setIsShared(Integer isShared) { this.isShared = isShared; }

    public LocalDateTime getShareTime() { return shareTime; }
    public void setShareTime(LocalDateTime shareTime) { this.shareTime = shareTime; }

    // ==================== 旧字段别名方法（向后兼容） ====================
    public String getTitle() { return eventName; }
    public void setTitle(String title) { this.eventName = title; }

    public String getSummary() { return riskDescription; }
    public void setSummary(String summary) { this.riskDescription = summary; }

    public String getUrl() { return sourceUrl; }
    public void setUrl(String url) { this.sourceUrl = url; }

    public String getSiteName() { return sourceWebsite; }
    public void setSiteName(String siteName) { this.sourceWebsite = siteName; }

    public String getSourceType() { return submissionChannel; }
    public void setSourceType(String sourceType) { this.submissionChannel = sourceType; }

    public String getReportUnit() { return submitOrgName; }
    public void setReportUnit(String reportUnit) { this.submitOrgName = reportUnit; }

    public String getRiskLevel() { return null; }
    public void setRiskLevel(String riskLevel) {}

    public Integer getReviewStatus() { return auditStatus; }
    public void setReviewStatus(Integer reviewStatus) { this.auditStatus = reviewStatus; }

    public List<String> getTags() { return classReportList; }
    public void setTags(List<String> tags) { this.classReportList = tags; }

    public List<String> getClassNameModel() { return classReportList; }
    public void setClassNameModel(List<String> classNameModel) { this.classReportList = classNameModel; }

    public List<String> getClassNameHuman() { return classHumanList; }
    public void setClassNameHuman(List<String> classNameHuman) { this.classHumanList = classNameHuman; }

    public LocalDateTime getCreatedTime() { return createTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createTime = createdTime; }

    public LocalDateTime getUpdatedTime() { return updateTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updateTime = updatedTime; }
}
