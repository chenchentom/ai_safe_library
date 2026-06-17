package com.aisafe.business.support;

import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 批量 ZIP 报告文件名解析：必须为「序号_名称.扩展名」
 */
@Component
public class ReportSerialFilenameMatcher {

    private static final Pattern DIGITS = Pattern.compile("^\\d+$");

    public Optional<Integer> parseSerial(String filename) {
        if (filename == null || filename.isBlank()) {
            return Optional.empty();
        }
        String baseName = Path.of(filename.trim()).getFileName().toString().trim();
        int underscore = baseName.indexOf('_');
        if (underscore <= 0) {
            return Optional.empty();
        }
        String prefix = baseName.substring(0, underscore).trim();
        if (!DIGITS.matcher(prefix).matches()) {
            return Optional.empty();
        }
        String remainder = baseName.substring(underscore + 1).trim();
        if (remainder.isEmpty()) {
            return Optional.empty();
        }
        int dot = remainder.lastIndexOf('.');
        if (dot <= 0) {
            return Optional.empty();
        }
        String namePart = remainder.substring(0, dot).trim();
        if (namePart.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(prefix));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public String invalidReason(String filename) {
        if (filename == null || filename.isBlank()) {
            return "文件名为空";
        }
        String baseName = Path.of(filename.trim()).getFileName().toString().trim();
        if (!baseName.contains("_")) {
            return "文件名须为「序号_名称.扩展名」格式";
        }
        Optional<Integer> serial = parseSerial(baseName);
        if (serial.isEmpty()) {
            return "无法解析序号，请使用如 1_风险报告.pdf 的命名";
        }
        try {
            ClueReportFileStorage.validateExtension(baseName);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
        return null;
    }
}
