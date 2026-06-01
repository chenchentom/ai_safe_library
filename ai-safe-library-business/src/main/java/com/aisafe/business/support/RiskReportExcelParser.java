package com.aisafe.business.support;

import com.aisafe.business.dto.RiskReportExcelRow;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 解析风险报送 Excel（17 列，与数据模版.xlsx 一致）
 */
public final class RiskReportExcelParser {

    private static final DateTimeFormatter[] DATE_TIME_FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
    };

    private RiskReportExcelParser() {
    }

    public static List<RiskReportExcelRow> parse(Path filePath) throws Exception {
        try (InputStream is = Files.newInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<RiskReportExcelRow> rows = new ArrayList<>();
            int lastRow = sheet.getLastRowNum();
            for (int i = 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isBlankRow(row)) {
                    continue;
                }
                RiskReportExcelRow parsed = parseRow(row, i + 1);
                rows.add(parsed);
            }
            return rows;
        }
    }

    private static RiskReportExcelRow parseRow(Row row, int rowNum) {
        RiskReportExcelRow data = new RiskReportExcelRow();
        data.setRowNum(rowNum);
        data.setNumber(parseInteger(row, 0));
        data.setEventName(getCellString(row, 1));
        data.setContent(getCellString(row, 2));
        data.setClassReport1(getCellString(row, 3));
        data.setClassReport2(getCellString(row, 4));
        data.setProductsComponentsServices(getCellString(row, 5));
        data.setOperatingEntity(getCellString(row, 6));
        data.setRiskDescription(getCellString(row, 7));
        data.setSourceUrl(getCellString(row, 8));
        data.setSourceWebsite(getCellString(row, 9));
        data.setPaperTitle(getCellString(row, 10));
        data.setResearchTeam(getCellString(row, 11));
        data.setRawIsVerifyText(getCellString(row, 12));
        data.setIsVerify(parseYesNo(data.getRawIsVerifyText()));
        data.setRawIsSubmitText(getCellString(row, 13));
        data.setIsSubmit(parseYesNo(data.getRawIsSubmitText()));
        data.setSubmissionChannel(getCellString(row, 14));
        data.setRawSubmissionTimeText(getCellString(row, 15));
        data.setSubmissionTime(parseDateTime(row, 15, data.getRawSubmissionTimeText()));
        data.setSubmitUserName(getCellString(row, 16));
        if (data.getIsSubmit() == null) {
            data.setIsSubmit(1);
        }
        return data;
    }

    private static boolean isBlankRow(Row row) {
        for (int i = 0; i <= 16; i++) {
            if (StringUtils.hasText(getCellString(row, i))) {
                return false;
            }
        }
        return true;
    }

    private static String getCellString(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) {
            return null;
        }
        DataFormatter formatter = new DataFormatter();
        String value = formatter.formatCellValue(cell);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static Integer parseInteger(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        }
        String text = getCellString(row, index);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Integer.parseInt(text.replace(".0", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parseYesNo(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        if ("是".equals(text) || "1".equals(text) || "true".equalsIgnoreCase(text) || "Y".equalsIgnoreCase(text)) {
            return 1;
        }
        if ("否".equals(text) || "0".equals(text) || "false".equalsIgnoreCase(text) || "N".equalsIgnoreCase(text)) {
            return 0;
        }
        return null;
    }

    private static LocalDateTime parseDateTime(Row row, int index, String text) {
        Cell cell = row.getCell(index);
        if (cell == null && !StringUtils.hasText(text)) {
            return null;
        }
        if (cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            Date date = cell.getDateCellValue();
            return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        }
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            double serial = Double.parseDouble(text);
            Date date = DateUtil.getJavaDate(serial);
            return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        } catch (NumberFormatException ignored) {
            // try string formats
        }
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                if (formatter.toString().contains("HH")) {
                    return LocalDateTime.parse(text, formatter);
                }
                LocalDate date = LocalDate.parse(text, formatter);
                return date.atStartOfDay();
            } catch (DateTimeParseException ignored) {
                // continue
            }
        }
        return null;
    }
}
