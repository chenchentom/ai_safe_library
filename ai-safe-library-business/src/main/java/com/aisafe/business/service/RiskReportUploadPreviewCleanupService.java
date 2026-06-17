package com.aisafe.business.service;

/**
 * 清理过期的批量上传预览会话
 */
public interface RiskReportUploadPreviewCleanupService {

    void cleanupExpiredPreviews();
}
