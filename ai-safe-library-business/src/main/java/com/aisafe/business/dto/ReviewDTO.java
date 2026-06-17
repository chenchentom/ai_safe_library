package com.aisafe.business.dto;

/**
 * 审核操作 DTO
 */
public class ReviewDTO {

    private String clueId;

    /** 是否入库：0=否，1=是 */
    private Integer isWarehouse;

    /** 人工审核风险类别路径，如 技术滥用风险/模型武器化风险 */
    private String riskCategory;

    /** 人工审核风险描述 */
    private String riskDescriptionHuman;

    /** 人工审核运营主体 */
    private String operatingEntityHuman;

    /** 审核备注 */
    private String reviewComment;

    /** 是否验证：0=否，1=是；不传则不更新 */
    private Integer isVerify;

    /** 是否报送：0=否，1=是；不传则不更新 */
    private Integer isSubmit;

    public String getClueId() { return clueId; }
    public void setClueId(String clueId) { this.clueId = clueId; }

    public Integer getIsWarehouse() { return isWarehouse; }
    public void setIsWarehouse(Integer isWarehouse) { this.isWarehouse = isWarehouse; }

    public String getRiskCategory() { return riskCategory; }
    public void setRiskCategory(String riskCategory) { this.riskCategory = riskCategory; }

    public String getRiskDescriptionHuman() { return riskDescriptionHuman; }
    public void setRiskDescriptionHuman(String riskDescriptionHuman) {
        this.riskDescriptionHuman = riskDescriptionHuman;
    }

    public String getOperatingEntityHuman() { return operatingEntityHuman; }
    public void setOperatingEntityHuman(String operatingEntityHuman) {
        this.operatingEntityHuman = operatingEntityHuman;
    }

    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }

    public Integer getIsVerify() { return isVerify; }
    public void setIsVerify(Integer isVerify) { this.isVerify = isVerify; }

    public Integer getIsSubmit() { return isSubmit; }
    public void setIsSubmit(Integer isSubmit) { this.isSubmit = isSubmit; }
}
