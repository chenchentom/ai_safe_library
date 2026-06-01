package com.aisafe.business.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

@Component
public class RiskReportFileStorage {

    private static final Pattern INVALID_CHARS = Pattern.compile("[\\\\/:*?\"<>|]");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final String baseDir;

    public RiskReportFileStorage(@Value("${aisafe.file.base-dir:}") String baseDir) {
        this.baseDir = baseDir;
    }

    public Path saveUploadFile(MultipartFile file, String deptName, String batchNo, LocalDateTime uploadTime)
            throws IOException {
        if (!StringUtils.hasText(baseDir)) {
            throw new IllegalStateException("未配置 aisafe.file.base-dir");
        }
        String safeDept = sanitizeDeptName(deptName);
        String monthFolder = uploadTime.format(MONTH_FMT);
        Path targetDir = Paths.get(baseDir.trim(), safeDept, monthFolder);
        Files.createDirectories(targetDir);

        String original = file.getOriginalFilename();
        String baseName = "upload";
        if (StringUtils.hasText(original)) {
            int dot = original.lastIndexOf('.');
            baseName = dot > 0 ? original.substring(0, dot) : original;
        }
        baseName = INVALID_CHARS.matcher(baseName).replaceAll("_");
        String shortBatch = batchNo != null && batchNo.length() >= 8 ? batchNo.substring(0, 8) : "batch";
        String fileName = baseName + "_" + uploadTime.format(TS_FMT) + "_" + shortBatch + ".xlsx";
        Path target = targetDir.resolve(fileName);
        file.transferTo(target.toFile());
        return target;
    }

    public static String sanitizeDeptName(String deptName) {
        if (!StringUtils.hasText(deptName)) {
            return "未分配部门";
        }
        return INVALID_CHARS.matcher(deptName.trim()).replaceAll("_");
    }
}
