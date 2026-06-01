package com.aisafe.business.service.impl;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class RiskReportUploadAsyncProcessor {

    private final RiskReportUploadServiceImpl uploadService;

    public RiskReportUploadAsyncProcessor(RiskReportUploadServiceImpl uploadService) {
        this.uploadService = uploadService;
    }

    @Async("riskReportUploadExecutor")
    public void process(Long batchId) {
        uploadService.processBatch(batchId);
    }
}
