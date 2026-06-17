package com.aisafe.business.support;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class ZipReportExtractor {

    private static final long MAX_ENTRY_BYTES = ClueReportFileStorage.MAX_FILE_BYTES;
    private static final long MAX_TOTAL_BYTES = ClueReportFileStorage.MAX_ZIP_BYTES;

    public List<Path> extract(Path zipFile, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        List<Path> extracted = new ArrayList<>();
        long totalBytes = 0;
        Set<String> usedNames = new HashSet<>();

        try (InputStream fis = Files.newInputStream(zipFile);
             ZipInputStream zis = new ZipInputStream(fis)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String entryName = entry.getName();
                if (isUnsafeEntry(entryName)) {
                    throw new IOException("ZIP 包含非法路径: " + entryName);
                }
                String fileName = Path.of(entryName).getFileName().toString();
                if (fileName.isBlank()) {
                    continue;
                }
                try {
                    ClueReportFileStorage.validateExtension(fileName);
                } catch (IllegalArgumentException ignored) {
                    continue;
                }
                String uniqueName = uniqueFileName(fileName, usedNames);
                Path target = targetDir.resolve(uniqueName).normalize();
                if (!target.startsWith(targetDir.normalize())) {
                    throw new IOException("ZIP 路径穿越: " + entryName);
                }
                long written = copyLimited(zis, target, MAX_ENTRY_BYTES);
                totalBytes += written;
                if (totalBytes > MAX_TOTAL_BYTES) {
                    throw new IOException("解压总大小超过限制");
                }
                usedNames.add(uniqueName.toLowerCase(Locale.ROOT));
                extracted.add(target);
                zis.closeEntry();
            }
        }
        return extracted;
    }

    private static String uniqueFileName(String fileName, Set<String> usedLower) {
        if (!usedLower.contains(fileName.toLowerCase(Locale.ROOT))) {
            return fileName;
        }
        String base;
        String ext;
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            base = fileName.substring(0, dot);
            ext = fileName.substring(dot);
        } else {
            base = fileName;
            ext = "";
        }
        int suffix = 2;
        while (true) {
            String candidate = base + "_" + suffix + ext;
            if (!usedLower.contains(candidate.toLowerCase(Locale.ROOT))) {
                return candidate;
            }
            suffix++;
        }
    }

    private static boolean isUnsafeEntry(String entryName) {
        if (entryName == null || entryName.isBlank()) {
            return true;
        }
        String normalized = entryName.replace('\\', '/');
        if (normalized.startsWith("/") || normalized.contains("..")) {
            return true;
        }
        return normalized.chars().anyMatch(c -> c == 0);
    }

    private static long copyLimited(InputStream in, Path target, long maxBytes) throws IOException {
        long total = 0;
        try (OutputStream out = Files.newOutputStream(target)) {
            byte[] buf = new byte[8192];
            int read;
            while ((read = in.read(buf)) >= 0) {
                total += read;
                if (total > maxBytes) {
                    Files.deleteIfExists(target);
                    throw new IOException("单个压缩包内文件超过大小限制: " + target.getFileName());
                }
                out.write(buf, 0, read);
            }
        }
        return total;
    }
}
