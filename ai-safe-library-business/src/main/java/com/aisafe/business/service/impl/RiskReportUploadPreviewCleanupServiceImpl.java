package com.aisafe.business.service.impl;

import com.aisafe.business.entity.BizRiskReportUploadPreview;
import com.aisafe.business.mapper.BizRiskReportUploadPreviewMapper;
import com.aisafe.business.service.RiskReportUploadPreviewCleanupService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
public class RiskReportUploadPreviewCleanupServiceImpl implements RiskReportUploadPreviewCleanupService {

    private static final Logger log = LoggerFactory.getLogger(RiskReportUploadPreviewCleanupServiceImpl.class);

    private final BizRiskReportUploadPreviewMapper previewMapper;

    public RiskReportUploadPreviewCleanupServiceImpl(BizRiskReportUploadPreviewMapper previewMapper) {
        this.previewMapper = previewMapper;
    }

    @Override
    @Scheduled(cron = "0 15 * * * ?")
    public void cleanupExpiredPreviews() {
        LocalDateTime now = LocalDateTime.now();
        List<BizRiskReportUploadPreview> expired = previewMapper.selectList(
                new LambdaQueryWrapper<BizRiskReportUploadPreview>()
                        .lt(BizRiskReportUploadPreview::getExpireTime, now));
        if (expired.isEmpty()) {
            return;
        }
        int removed = 0;
        for (BizRiskReportUploadPreview preview : expired) {
            try {
                deletePreviewAssets(preview);
                previewMapper.deleteById(preview.getId());
                removed++;
            } catch (Exception e) {
                log.warn("清理预览会话失败 token={}", preview.getPreviewToken(), e);
            }
        }
        log.info("清理过期上传预览会话 {} 条", removed);
    }

    private void deletePreviewAssets(BizRiskReportUploadPreview preview) throws IOException {
        deleteIfExists(preview.getExcelPath());
        deleteIfExists(preview.getZipPath());
        if (StringUtils.hasText(preview.getExtractedDir())) {
            deleteDirectoryRecursively(Path.of(preview.getExtractedDir()));
        }
    }

    private static void deleteIfExists(String filePath) throws IOException {
        if (!StringUtils.hasText(filePath)) {
            return;
        }
        Files.deleteIfExists(Path.of(filePath));
    }

    private static void deleteDirectoryRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            });
        }
    }
}
