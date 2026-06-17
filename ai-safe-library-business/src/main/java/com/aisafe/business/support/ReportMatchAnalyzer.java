package com.aisafe.business.support;

import com.aisafe.business.dto.ReportMatchPreviewResult;
import com.aisafe.business.dto.RiskReportExcelRow;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Component
public class ReportMatchAnalyzer {

    private final ReportSerialFilenameMatcher filenameMatcher;

    public ReportMatchAnalyzer(ReportSerialFilenameMatcher filenameMatcher) {
        this.filenameMatcher = filenameMatcher;
    }

    public ReportMatchPreviewResult analyze(List<RiskReportExcelRow> rows, Path extractedDir) {
        ReportMatchPreviewResult result = new ReportMatchPreviewResult();
        result.setExcelRowCount(rows.size());

        Map<Integer, List<String>> matchedFilesBySerial = new HashMap<>();
        List<ReportMatchPreviewResult.UnmatchedFile> unmatched = new ArrayList<>();
        int validReportCount = 0;

        if (extractedDir != null && Files.isDirectory(extractedDir)) {
            try (var stream = Files.list(extractedDir)) {
                List<Path> files = stream.filter(Files::isRegularFile).sorted().toList();
                for (Path file : files) {
                    String name = file.getFileName().toString();
                    String invalidReason = filenameMatcher.invalidReason(name);
                    if (invalidReason != null) {
                        unmatched.add(new ReportMatchPreviewResult.UnmatchedFile(name, invalidReason));
                        continue;
                    }
                    validReportCount++;
                    Integer serial = filenameMatcher.parseSerial(name).orElse(null);
                    if (serial == null) {
                        unmatched.add(new ReportMatchPreviewResult.UnmatchedFile(name, "无法解析序号"));
                        continue;
                    }
                    matchedFilesBySerial.computeIfAbsent(serial, k -> new ArrayList<>()).add(name);
                }
            } catch (Exception e) {
                throw new IllegalStateException("分析 ZIP 报告失败: " + e.getMessage(), e);
            }
        }

        Set<Integer> excelSerials = new TreeSet<>();
        for (RiskReportExcelRow row : rows) {
            if (row.getNumber() != null) {
                excelSerials.add(row.getNumber());
            }
        }

        List<Integer> missing = new ArrayList<>();
        List<ReportMatchPreviewResult.MatchedPreviewRow> previewRows = new ArrayList<>();
        int matchedSerialCount = 0;

        for (RiskReportExcelRow row : rows) {
            Integer serial = row.getNumber();
            if (serial == null) {
                continue;
            }
            List<String> files = matchedFilesBySerial.getOrDefault(serial, List.of());
            if (files.isEmpty()) {
                if (extractedDir != null) {
                    missing.add(serial);
                }
            } else {
                matchedSerialCount++;
                ReportMatchPreviewResult.MatchedPreviewRow previewRow = new ReportMatchPreviewResult.MatchedPreviewRow();
                previewRow.setSerial(serial);
                previewRow.setEventName(row.getEventName());
                previewRow.setFiles(new ArrayList<>(files));
                previewRows.add(previewRow);
            }
        }

        Set<Integer> matchedSerialSet = matchedFilesBySerial.keySet();
        for (Integer serial : matchedSerialSet) {
            if (!excelSerials.contains(serial)) {
                for (String fileName : matchedFilesBySerial.get(serial)) {
                    unmatched.add(new ReportMatchPreviewResult.UnmatchedFile(
                            fileName, "Excel 中无序号 " + serial));
                }
            }
        }

        result.setValidReportCount(validReportCount);
        result.setMatchedSerialCount(matchedSerialCount);
        result.setMissingReportSerials(missing);
        result.setUnmatchedFiles(unmatched);
        result.setMatchedPreview(previewRows);
        return result;
    }
}
