package com.aisafe.business.document;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 风险审核记录 ES 文档（索引 biz_risk_review_record，字段 snake_case）
 */
@Document(indexName = "biz_risk_review_record")
public class BizRiskReviewRecord {

    @Id
    private String id;

    @Field(name = "clue_id", type = FieldType.Keyword)
    private String clueId;

    @Field(name = "is_warehouse", type = FieldType.Integer)
    private Integer isWarehouse;

    @Field(name = "class_human_1", type = FieldType.Keyword)
    private String classHuman1;

    @Field(name = "class_human_2", type = FieldType.Keyword)
    private String classHuman2;

    @Field(name = "class_human_list", type = FieldType.Keyword)
    private List<String> classHumanList;

    /** 风险类别完整路径，如 一级/二级 */
    @Field(name = "risk_category", type = FieldType.Keyword)
    private String riskCategory;

    @Field(name = "risk_description_human", type = FieldType.Text, analyzer = "standard")
    private String riskDescriptionHuman;

    @Field(name = "operating_entity_human", type = FieldType.Keyword)
    private String operatingEntityHuman;

    @Field(name = "review_result", type = FieldType.Keyword)
    private String reviewResult;

    @Field(name = "review_comment", type = FieldType.Text, analyzer = "standard")
    private String reviewComment;

    @Field(name = "reviewer", type = FieldType.Keyword)
    private String reviewer;

    @Field(name = "reviewer_name", type = FieldType.Keyword)
    private String reviewerName;

    @Field(name = "review_dept", type = FieldType.Keyword)
    private String reviewDept;

    @Field(name = "review_time", type = FieldType.Date, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime reviewTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getClueId() { return clueId; }
    public void setClueId(String clueId) { this.clueId = clueId; }

    public Integer getIsWarehouse() { return isWarehouse; }
    public void setIsWarehouse(Integer isWarehouse) { this.isWarehouse = isWarehouse; }

    public String getClassHuman1() { return classHuman1; }
    public void setClassHuman1(String classHuman1) { this.classHuman1 = classHuman1; }

    public String getClassHuman2() { return classHuman2; }
    public void setClassHuman2(String classHuman2) { this.classHuman2 = classHuman2; }

    public List<String> getClassHumanList() { return classHumanList; }
    public void setClassHumanList(List<String> classHumanList) { this.classHumanList = classHumanList; }

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

    public String getReviewResult() { return reviewResult; }
    public void setReviewResult(String reviewResult) { this.reviewResult = reviewResult; }

    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }

    public String getReviewer() { return reviewer; }
    public void setReviewer(String reviewer) { this.reviewer = reviewer; }

    public String getReviewerName() { return reviewerName; }
    public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }

    public String getReviewDept() { return reviewDept; }
    public void setReviewDept(String reviewDept) { this.reviewDept = reviewDept; }

    public LocalDateTime getReviewTime() { return reviewTime; }
    public void setReviewTime(LocalDateTime reviewTime) { this.reviewTime = reviewTime; }
}
