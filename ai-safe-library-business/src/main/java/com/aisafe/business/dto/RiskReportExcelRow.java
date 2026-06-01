package com.aisafe.business.dto;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 风险报送 Excel 单行解析结果（17 列模板）
 */
public class RiskReportExcelRow {

    private int rowNum;
    private Integer number;
    private String eventName;
    private String content;
    private String classReport1;
    private String classReport2;
    private String productsComponentsServices;
    private String operatingEntity;
    private String riskDescription;
    private String sourceUrl;
    private String sourceWebsite;
    private String paperTitle;
    private String researchTeam;
    private Integer isVerify;
    private Integer isSubmit;
    private String submissionChannel;
    private LocalDateTime submissionTime;
    private String submitUserName;
    private String rawIsVerifyText;
    private String rawIsSubmitText;
    private String rawSubmissionTimeText;

    public int getRowNum() { return rowNum; }
    public void setRowNum(int rowNum) { this.rowNum = rowNum; }

    public Integer getNumber() { return number; }
    public void setNumber(Integer number) { this.number = number; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getClassReport1() { return classReport1; }
    public void setClassReport1(String classReport1) { this.classReport1 = classReport1; }

    public String getClassReport2() { return classReport2; }
    public void setClassReport2(String classReport2) { this.classReport2 = classReport2; }

    public String getProductsComponentsServices() { return productsComponentsServices; }
    public void setProductsComponentsServices(String productsComponentsServices) {
        this.productsComponentsServices = productsComponentsServices;
    }

    public String getOperatingEntity() { return operatingEntity; }
    public void setOperatingEntity(String operatingEntity) { this.operatingEntity = operatingEntity; }

    public String getRiskDescription() { return riskDescription; }
    public void setRiskDescription(String riskDescription) { this.riskDescription = riskDescription; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public String getSourceWebsite() { return sourceWebsite; }
    public void setSourceWebsite(String sourceWebsite) { this.sourceWebsite = sourceWebsite; }

    public String getPaperTitle() { return paperTitle; }
    public void setPaperTitle(String paperTitle) { this.paperTitle = paperTitle; }

    public String getResearchTeam() { return researchTeam; }
    public void setResearchTeam(String researchTeam) { this.researchTeam = researchTeam; }

    public Integer getIsVerify() { return isVerify; }
    public void setIsVerify(Integer isVerify) { this.isVerify = isVerify; }

    public Integer getIsSubmit() { return isSubmit; }
    public void setIsSubmit(Integer isSubmit) { this.isSubmit = isSubmit; }

    public String getSubmissionChannel() { return submissionChannel; }
    public void setSubmissionChannel(String submissionChannel) { this.submissionChannel = submissionChannel; }

    public LocalDateTime getSubmissionTime() { return submissionTime; }
    public void setSubmissionTime(LocalDateTime submissionTime) { this.submissionTime = submissionTime; }

    public String getSubmitUserName() { return submitUserName; }
    public void setSubmitUserName(String submitUserName) { this.submitUserName = submitUserName; }

    public String getRawIsVerifyText() { return rawIsVerifyText; }
    public void setRawIsVerifyText(String rawIsVerifyText) { this.rawIsVerifyText = rawIsVerifyText; }

    public String getRawIsSubmitText() { return rawIsSubmitText; }
    public void setRawIsSubmitText(String rawIsSubmitText) { this.rawIsSubmitText = rawIsSubmitText; }

    public String getRawSubmissionTimeText() { return rawSubmissionTimeText; }
    public void setRawSubmissionTimeText(String rawSubmissionTimeText) { this.rawSubmissionTimeText = rawSubmissionTimeText; }

    public Map<String, Object> toRawMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("number", number);
        map.put("eventName", eventName);
        map.put("content", content);
        map.put("classReport1", classReport1);
        map.put("classReport2", classReport2);
        map.put("productsComponentsServices", productsComponentsServices);
        map.put("operatingEntity", operatingEntity);
        map.put("riskDescription", riskDescription);
        map.put("sourceUrl", sourceUrl);
        map.put("sourceWebsite", sourceWebsite);
        map.put("paperTitle", paperTitle);
        map.put("researchTeam", researchTeam);
        map.put("isVerify", isVerify);
        map.put("isSubmit", isSubmit);
        map.put("submissionChannel", submissionChannel);
        map.put("submissionTime", submissionTime);
        map.put("submitUserName", submitUserName);
        return map;
    }
}
