package com.aisafe.business.support;

import com.aisafe.business.dto.RiskClueSearchQuery;

import java.util.Map;

/**
 * 风险线索搜索参数解析（供线索库与风险报送共用）
 */
public final class RiskClueSearchSupport {

    private RiskClueSearchSupport() {
    }

    public static RiskClueSearchQuery buildSearchQuery(
            String keyword,
            String riskCategory,
            Object reviewStatus,
            String sourceWebsite,
            String operatingEntity,
            String submissionChannel,
            String productsComponentsServices,
            String submissionStartTime,
            String submissionEndTime,
            Object isWarehouse,
            String auditRiskCategory,
            String operatingEntityHuman,
            String auditUserName,
            String auditStartTime,
            String auditEndTime,
            String submitUserName,
            String submitOrgName,
            String sourceType,
            String legacyStartTime,
            String legacyEndTime,
            int page,
            int size,
            String reportUnit) {

        RiskClueSearchQuery query = new RiskClueSearchQuery();
        query.setKeyword(keyword);
        query.setRiskCategory(riskCategory);
        query.setReviewStatus(parseReviewStatus(reviewStatus));
        query.setSourceWebsite(sourceWebsite);
        query.setOperatingEntity(operatingEntity);
        query.setSubmissionChannel(submissionChannel);
        query.setProductsComponentsServices(productsComponentsServices);
        query.setSubmissionStartTime(firstNonBlank(submissionStartTime, legacyStartTime));
        query.setSubmissionEndTime(firstNonBlank(submissionEndTime, legacyEndTime));
        query.setIsWarehouse(parseIsWarehouse(isWarehouse));
        query.setAuditRiskCategory(auditRiskCategory);
        query.setOperatingEntityHuman(operatingEntityHuman);
        query.setAuditUserName(auditUserName);
        query.setAuditStartTime(auditStartTime);
        query.setAuditEndTime(auditEndTime);
        query.setSubmitUserName(submitUserName);
        query.setSubmitOrgName(submitOrgName);
        query.setSourceType(sourceType);
        query.setPage(page);
        query.setSize(size);
        query.setReportUnit(reportUnit);
        return query;
    }

    public static RiskClueSearchQuery buildFromMap(Map<String, Object> body, String reportUnit) {
        if (body == null) {
            body = Map.of();
        }
        RiskClueSearchQuery query = buildSearchQuery(
                (String) body.get("keyword"),
                (String) body.get("riskCategory"),
                body.get("reviewStatus"),
                (String) body.get("sourceWebsite"),
                (String) body.get("operatingEntity"),
                (String) body.get("submissionChannel"),
                (String) body.get("productsComponentsServices"),
                firstNonBlank((String) body.get("submissionStartTime"), (String) body.get("startTime")),
                firstNonBlank((String) body.get("submissionEndTime"), (String) body.get("endTime")),
                body.get("isWarehouse"),
                (String) body.get("auditRiskCategory"),
                (String) body.get("operatingEntityHuman"),
                (String) body.get("auditUserName"),
                (String) body.get("auditStartTime"),
                (String) body.get("auditEndTime"),
                (String) body.get("submitUserName"),
                (String) body.get("submitOrgName"),
                (String) body.get("sourceType"),
                (String) body.get("startTime"),
                (String) body.get("endTime"),
                body.get("page") != null ? ((Number) body.get("page")).intValue() : 1,
                body.get("size") != null ? ((Number) body.get("size")).intValue() : 10,
                reportUnit);
        applyYesNoFilters(query, body);
        return query;
    }

    public static void applyYesNoFilters(RiskClueSearchQuery query, Map<String, Object> body) {
        if (query == null || body == null) {
            return;
        }
        query.setIsVerify(parseReviewStatus(body.get("isVerify")));
        query.setIsSubmit(parseReviewStatus(body.get("isSubmit")));
    }

    public static void applyYesNoFilters(RiskClueSearchQuery query, Object isVerify, Object isSubmit) {
        if (query == null) {
            return;
        }
        query.setIsVerify(parseReviewStatus(isVerify));
        query.setIsSubmit(parseReviewStatus(isSubmit));
    }

    public static String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.trim().isEmpty()) {
            return primary;
        }
        return fallback;
    }

    public static Integer parseReviewStatus(Object reviewStatus) {
        if (reviewStatus == null) {
            return null;
        }
        if (reviewStatus instanceof Integer) {
            return (Integer) reviewStatus;
        }
        if (reviewStatus instanceof Number) {
            return ((Number) reviewStatus).intValue();
        }
        if (reviewStatus instanceof String) {
            String str = (String) reviewStatus;
            if (str.trim().isEmpty()) {
                return null;
            }
            try {
                return Integer.parseInt(str.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public static Integer parseIsWarehouse(Object isWarehouse) {
        if (isWarehouse == null) {
            return null;
        }
        if (isWarehouse instanceof Boolean) {
            return Boolean.TRUE.equals(isWarehouse) ? 1 : 0;
        }
        return parseReviewStatus(isWarehouse);
    }
}
