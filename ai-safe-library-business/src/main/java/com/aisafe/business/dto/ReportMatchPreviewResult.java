package com.aisafe.business.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReportMatchPreviewResult {

    private int excelRowCount;
    private int validReportCount;
    private int matchedSerialCount;
    private List<Integer> missingReportSerials = new ArrayList<>();
    private List<UnmatchedFile> unmatchedFiles = new ArrayList<>();
    private List<MatchedPreviewRow> matchedPreview = new ArrayList<>();
    private List<InvalidRow> invalidRows = new ArrayList<>();
    private String previewToken;

    public int getExcelRowCount() { return excelRowCount; }
    public void setExcelRowCount(int excelRowCount) { this.excelRowCount = excelRowCount; }

    public int getValidReportCount() { return validReportCount; }
    public void setValidReportCount(int validReportCount) { this.validReportCount = validReportCount; }

    public int getMatchedSerialCount() { return matchedSerialCount; }
    public void setMatchedSerialCount(int matchedSerialCount) { this.matchedSerialCount = matchedSerialCount; }

    public List<Integer> getMissingReportSerials() { return missingReportSerials; }
    public void setMissingReportSerials(List<Integer> missingReportSerials) { this.missingReportSerials = missingReportSerials; }

    public List<UnmatchedFile> getUnmatchedFiles() { return unmatchedFiles; }
    public void setUnmatchedFiles(List<UnmatchedFile> unmatchedFiles) { this.unmatchedFiles = unmatchedFiles; }

    public List<MatchedPreviewRow> getMatchedPreview() { return matchedPreview; }
    public void setMatchedPreview(List<MatchedPreviewRow> matchedPreview) { this.matchedPreview = matchedPreview; }

    public List<InvalidRow> getInvalidRows() { return invalidRows; }
    public void setInvalidRows(List<InvalidRow> invalidRows) { this.invalidRows = invalidRows; }

    public String getPreviewToken() { return previewToken; }
    public void setPreviewToken(String previewToken) { this.previewToken = previewToken; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("excelRowCount", excelRowCount);
        map.put("validReportCount", validReportCount);
        map.put("matchedSerialCount", matchedSerialCount);
        map.put("missingReportSerials", missingReportSerials);
        map.put("unmatchedFiles", unmatchedFiles.stream().map(UnmatchedFile::toMap).toList());
        map.put("matchedPreview", matchedPreview.stream().map(MatchedPreviewRow::toMap).toList());
        map.put("invalidRows", invalidRows.stream().map(InvalidRow::toMap).toList());
        map.put("previewToken", previewToken);
        return map;
    }

    public static class UnmatchedFile {
        private String fileName;
        private String reason;

        public UnmatchedFile() {
        }

        public UnmatchedFile(String fileName, String reason) {
            this.fileName = fileName;
            this.reason = reason;
        }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("fileName", fileName);
            map.put("reason", reason);
            return map;
        }
    }

    public static class MatchedPreviewRow {
        private Integer serial;
        private String eventName;
        private List<String> files = new ArrayList<>();

        public Integer getSerial() { return serial; }
        public void setSerial(Integer serial) { this.serial = serial; }

        public String getEventName() { return eventName; }
        public void setEventName(String eventName) { this.eventName = eventName; }

        public List<String> getFiles() { return files; }
        public void setFiles(List<String> files) { this.files = files; }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("serial", serial);
            map.put("eventName", eventName);
            map.put("files", files);
            return map;
        }
    }

    public static class InvalidRow {
        private Integer rowNum;
        private Integer serial;
        private String errorMessage;

        public InvalidRow() {
        }

        public InvalidRow(Integer rowNum, Integer serial, String errorMessage) {
            this.rowNum = rowNum;
            this.serial = serial;
            this.errorMessage = errorMessage;
        }

        public Integer getRowNum() { return rowNum; }
        public void setRowNum(Integer rowNum) { this.rowNum = rowNum; }

        public Integer getSerial() { return serial; }
        public void setSerial(Integer serial) { this.serial = serial; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("rowNum", rowNum);
            map.put("serial", serial);
            map.put("errorMessage", errorMessage);
            return map;
        }
    }
}
