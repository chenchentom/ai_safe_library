package com.aisafe.business.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量审核结果
 */
public class BatchReviewResult {

    /** 符合筛选条件的未审核线索总数 */
    private int total;

    /** 成功审核数量 */
    private int success;

    /** 失败数量 */
    private int failed;

    /** 实际处理数量（成功 + 失败） */
    private int processedCount;

    /** 失败明细是否被截断（超过返回上限） */
    private boolean failuresTruncated;

    /** 失败明细（最多返回前 100 条） */
    private List<FailureItem> failures = new ArrayList<>();

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getSuccess() {
        return success;
    }

    public void setSuccess(int success) {
        this.success = success;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public int getProcessedCount() {
        return processedCount;
    }

    public void setProcessedCount(int processedCount) {
        this.processedCount = processedCount;
    }

    public boolean isFailuresTruncated() {
        return failuresTruncated;
    }

    public void setFailuresTruncated(boolean failuresTruncated) {
        this.failuresTruncated = failuresTruncated;
    }

    public List<FailureItem> getFailures() {
        return failures;
    }

    public void setFailures(List<FailureItem> failures) {
        this.failures = failures;
    }

    public static class FailureItem {
        private String clueId;
        private String eventName;
        private String reason;

        public FailureItem() {
        }

        public FailureItem(String clueId, String eventName, String reason) {
            this.clueId = clueId;
            this.eventName = eventName;
            this.reason = reason;
        }

        public String getClueId() {
            return clueId;
        }

        public void setClueId(String clueId) {
            this.clueId = clueId;
        }

        public String getEventName() {
            return eventName;
        }

        public void setEventName(String eventName) {
            this.eventName = eventName;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
