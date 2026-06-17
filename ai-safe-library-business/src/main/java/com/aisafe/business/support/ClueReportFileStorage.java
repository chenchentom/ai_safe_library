package com.aisafe.business.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class ClueReportFileStorage {

    public static final long MAX_FILE_BYTES = 20L * 1024 * 1024;
    public static final long MAX_ZIP_BYTES = 200L * 1024 * 1024;
    public static final int MAX_FILES_PER_CLUE = 10;

    private static final Pattern INVALID_CHARS = Pattern.compile("[\\\\/:*?\"<>|]");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static final Set<String> ALLOWED_EXT = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "png", "jpg", "jpeg"
    );

    private final String baseDir;

    public ClueReportFileStorage(@Value("${aisafe.file.base-dir:}") String baseDir) {
        this.baseDir = baseDir;
    }

    public Path requireBaseDir() {
        if (!StringUtils.hasText(baseDir)) {
            throw new IllegalStateException("未配置 aisafe.file.base-dir");
        }
        return Paths.get(baseDir.trim());
    }

    public Path previewSessionDir(String previewToken, LocalDateTime time) {
        Path dir = requireBaseDir()
                .resolve("clue-reports")
                .resolve("preview")
                .resolve(time.format(MONTH_FMT))
                .resolve(previewToken);
        return ensureDir(dir);
    }

    public Path batchRootDir(String deptName, String batchNo, LocalDateTime time) {
        Path dir = requireBaseDir()
                .resolve("clue-reports")
                .resolve("batch")
                .resolve(RiskReportFileStorage.sanitizeDeptName(deptName))
                .resolve(time.format(MONTH_FMT))
                .resolve(batchNo);
        return ensureDir(dir);
    }

    public Path batchExtractDir(String deptName, String batchNo, LocalDateTime time) {
        Path dir = requireBaseDir()
                .resolve("clue-reports")
                .resolve("batch")
                .resolve(RiskReportFileStorage.sanitizeDeptName(deptName))
                .resolve(time.format(MONTH_FMT))
                .resolve(batchNo)
                .resolve("extracted");
        return ensureDir(dir);
    }

    public Path clueAttachmentDir(String deptName, String clueId, LocalDateTime time) {
        Path dir = requireBaseDir()
                .resolve("clue-reports")
                .resolve("manual")
                .resolve(RiskReportFileStorage.sanitizeDeptName(deptName))
                .resolve(time.format(MONTH_FMT))
                .resolve(clueId);
        return ensureDir(dir);
    }

    public Path saveMultipart(MultipartFile file, Path targetDir) throws IOException {
        validateMultipart(file);
        String original = sanitizeOriginalName(file.getOriginalFilename());
        String stored = UUID.randomUUID().toString().replace("-", "")
                + "_" + LocalDateTime.now().format(TS_FMT)
                + getExtension(original);
        Path target = targetDir.resolve(stored);
        file.transferTo(target.toFile());
        return target;
    }

    public Path copyToClueDir(Path source, Path clueDir, String originalName) throws IOException {
        validateExtension(originalName);
        if (!Files.isRegularFile(source)) {
            throw new IOException("源文件不存在");
        }
        long size = Files.size(source);
        if (size > MAX_FILE_BYTES) {
            throw new IOException("单文件不能超过 20MB");
        }
        String safeOriginal = sanitizeOriginalName(originalName);
        String stored = UUID.randomUUID().toString().replace("-", "")
                + "_" + LocalDateTime.now().format(TS_FMT)
                + getExtension(safeOriginal);
        Path target = clueDir.resolve(stored);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    public void validateMultipart(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("单文件不能超过 20MB");
        }
        validateExtension(file.getOriginalFilename());
    }

    public void validateZipMultipart(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("ZIP 不能为空");
        }
        if (file.getSize() > MAX_ZIP_BYTES) {
            throw new IllegalArgumentException("ZIP 不能超过 200MB");
        }
        String name = file.getOriginalFilename();
        if (!StringUtils.hasText(name) || !name.toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("报告压缩包须为 .zip 格式");
        }
    }

    public static void validateExtension(String filename) {
        String ext = extensionOf(filename);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new IllegalArgumentException("不支持的文件类型: " + ext);
        }
    }

    public static String contentTypeOf(String filename) {
        String ext = extensionOf(filename);
        return switch (ext) {
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            default -> "application/octet-stream";
        };
    }

    public static String sanitizeOriginalName(String original) {
        if (!StringUtils.hasText(original)) {
            return "report.bin";
        }
        String name = Paths.get(original.trim()).getFileName().toString();
        return INVALID_CHARS.matcher(name).replaceAll("_");
    }

    public static String extensionOf(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        String name = Paths.get(filename.trim()).getFileName().toString().toLowerCase();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(dot + 1) : "";
    }

    private static String getExtension(String filename) {
        String ext = extensionOf(filename);
        return StringUtils.hasText(ext) ? "." + ext : "";
    }

    private static Path ensureDir(Path dir) {
        try {
            Files.createDirectories(dir);
            return dir;
        } catch (IOException e) {
            throw new IllegalStateException("创建目录失败: " + dir, e);
        }
    }

    public static void validateStreamSize(InputStream in, long maxBytes) throws IOException {
        long total = 0;
        byte[] buf = new byte[8192];
        int read;
        while ((read = in.read(buf)) >= 0) {
            total += read;
            if (total > maxBytes) {
                throw new IOException("解压内容超过大小限制");
            }
        }
    }
}
