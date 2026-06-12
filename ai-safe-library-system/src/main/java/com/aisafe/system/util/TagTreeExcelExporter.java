package com.aisafe.system.util;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 树形标签 Excel 导出
 */
public final class TagTreeExcelExporter {

    private static final String[] HEADERS = {
            "标签ID", "父级标签", "标签名称", "标签编码", "层级", "路径", "排序", "状态", "描述", "图标", "所属模块"
    };

    private TagTreeExcelExporter() {
    }

    public record TagExcelRow(
            String id,
            String parentName,
            String tagName,
            String tagCode,
            Integer tagLevel,
            String tagPath,
            Integer sortOrder,
            String status,
            String description,
            String icon,
            String module
    ) {
    }

    public static void write(HttpServletResponse response, String fileName, List<TagExcelRow> rows)
            throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("标签列表");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                headerRow.createCell(i).setCellValue(HEADERS[i]);
            }

            int rowIndex = 1;
            for (TagExcelRow row : rows) {
                Row dataRow = sheet.createRow(rowIndex++);
                dataRow.createCell(0).setCellValue(nullToEmpty(row.id()));
                dataRow.createCell(1).setCellValue(nullToEmpty(row.parentName()));
                dataRow.createCell(2).setCellValue(nullToEmpty(row.tagName()));
                dataRow.createCell(3).setCellValue(nullToEmpty(row.tagCode()));
                dataRow.createCell(4).setCellValue(row.tagLevel() != null ? row.tagLevel() : 0);
                dataRow.createCell(5).setCellValue(nullToEmpty(row.tagPath()));
                dataRow.createCell(6).setCellValue(row.sortOrder() != null ? row.sortOrder() : 0);
                dataRow.createCell(7).setCellValue(formatStatus(row.status()));
                dataRow.createCell(8).setCellValue(nullToEmpty(row.description()));
                dataRow.createCell(9).setCellValue(nullToEmpty(row.icon()));
                dataRow.createCell(10).setCellValue(nullToEmpty(row.module()));
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);
            workbook.write(response.getOutputStream());
            response.flushBuffer();
        }
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    private static String formatStatus(String status) {
        if (status == null) {
            return "";
        }
        return "0".equals(status) ? "启用" : "1".equals(status) ? "停用" : status;
    }
}
