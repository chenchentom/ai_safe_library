package com.aisafe.business.dto;

/**
 * 手动新增风险线索 / 安全事件
 */
public class RiskClueManualCreateDTO {

    /** 事件名称（必填） */
    private String eventName;

    /** 风险描述（必填） */
    private String riskDescription;

    /** 全文内容（选填） */
    private String content;

    /** 报送类别，格式：一级/二级 */
    private String riskCategory;

    /** 审核类别（安全事件入库必填），格式：一级/二级 */
    private String auditRiskCategory;

    /** 来源链接 */
    private String sourceUrl;

    /** 来源网站 */
    private String sourceWebsite;

    /** 论文名称 */
    private String paperTitle;

    /** 研究团队 */
    private String researchTeam;

    /** 报送渠道 */
    private String submissionChannel;

    /** 运营主体（报送） */
    private String operatingEntity;

    /** 产品/组件/服务 */
    private String productsComponentsServices;

    /** 运营主体（审核） */
    private String operatingEntityHuman;

    /** 风险描述（审核） */
    private String riskDescriptionHuman;

    /** 报送时间 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss */
    private String submissionTime;

    /** 报送单位（不填则取当前用户部门） */
    private String submitOrgName;

    /** 报送人（不填则取当前用户昵称） */
    private String submitUserName;

    /** 是否入库：1 入库 0 不入库（仅安全事件创建时有效，默认 1） */
    private Integer isWarehouse;

    /** 审核备注 */
    private String reviewComment;

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public String getRiskDescription() { return riskDescription; }
    public void setRiskDescription(String riskDescription) { this.riskDescription = riskDescription; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getRiskCategory() { return riskCategory; }
    public void setRiskCategory(String riskCategory) { this.riskCategory = riskCategory; }

    public String getAuditRiskCategory() { return auditRiskCategory; }
    public void setAuditRiskCategory(String auditRiskCategory) { this.auditRiskCategory = auditRiskCategory; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public String getSourceWebsite() { return sourceWebsite; }
    public void setSourceWebsite(String sourceWebsite) { this.sourceWebsite = sourceWebsite; }

    public String getPaperTitle() { return paperTitle; }
    public void setPaperTitle(String paperTitle) { this.paperTitle = paperTitle; }

    public String getResearchTeam() { return researchTeam; }
    public void setResearchTeam(String researchTeam) { this.researchTeam = researchTeam; }

    public String getSubmissionChannel() { return submissionChannel; }
    public void setSubmissionChannel(String submissionChannel) { this.submissionChannel = submissionChannel; }

    public String getOperatingEntity() { return operatingEntity; }
    public void setOperatingEntity(String operatingEntity) { this.operatingEntity = operatingEntity; }

    public String getProductsComponentsServices() { return productsComponentsServices; }
    public void setProductsComponentsServices(String productsComponentsServices) {
        this.productsComponentsServices = productsComponentsServices;
    }

    public String getOperatingEntityHuman() { return operatingEntityHuman; }
    public void setOperatingEntityHuman(String operatingEntityHuman) { this.operatingEntityHuman = operatingEntityHuman; }

    public String getRiskDescriptionHuman() { return riskDescriptionHuman; }
    public void setRiskDescriptionHuman(String riskDescriptionHuman) { this.riskDescriptionHuman = riskDescriptionHuman; }

    public String getSubmissionTime() { return submissionTime; }
    public void setSubmissionTime(String submissionTime) { this.submissionTime = submissionTime; }

    public String getSubmitOrgName() { return submitOrgName; }
    public void setSubmitOrgName(String submitOrgName) { this.submitOrgName = submitOrgName; }

    public String getSubmitUserName() { return submitUserName; }
    public void setSubmitUserName(String submitUserName) { this.submitUserName = submitUserName; }

    public Integer getIsWarehouse() { return isWarehouse; }
    public void setIsWarehouse(Integer isWarehouse) { this.isWarehouse = isWarehouse; }

    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }
}
