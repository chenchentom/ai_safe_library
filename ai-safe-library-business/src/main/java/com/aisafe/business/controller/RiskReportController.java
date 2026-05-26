package com.aisafe.business.controller;

import com.aisafe.business.document.BizRiskClue;
import com.aisafe.business.service.RiskReportService;
import com.aisafe.common.result.R;
import com.aisafe.system.entity.SysDept;
import com.aisafe.system.entity.SysUser;
import com.aisafe.system.service.ISysDeptService;
import com.aisafe.system.service.ISysUserService;
import cn.dev33.satoken.stp.StpUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 风险报送控制器
 */
@RestController
@RequestMapping("/business/risk-report")
public class RiskReportController {

    private final RiskReportService riskReportService;
    private final ISysUserService userService;
    private final ISysDeptService deptService;

    public RiskReportController(RiskReportService riskReportService,
                                ISysUserService userService,
                                ISysDeptService deptService) {
        this.riskReportService = riskReportService;
        this.userService = userService;
        this.deptService = deptService;
    }

    /**
     * 获取我的报送线索
     */
    @GetMapping("/my")
    public R<Map<String, Object>> myReports(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        String reportUnit = getCurrentDeptName();
        Map<String, Object> result = riskReportService.getMyReports(reportUnit, page, size);
        return R.ok(result);
    }

    /**
     * 上传 Excel 批量导入报送线索
     * Excel 列: title, content, url, source_type, risk_level, summary
     */
    @PostMapping("/upload")
    public R<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        try {
            String reportUnit = getCurrentDeptName();
            List<BizRiskClue> clues = parseExcel(file);

            riskReportService.batchImport(clues, reportUnit);

            Map<String, Object> data = new HashMap<>();
            data.put("imported", clues.size());
            return R.ok("导入成功", data);
        } catch (Exception e) {
            return R.fail("导入失败: " + e.getMessage());
        }
    }

    /**
     * 获取我的报送统计
     */
    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        String reportUnit = getCurrentDeptName();
        Map<String, Object> result = riskReportService.getMyReports(reportUnit, 1, 1);
        long total = (long) result.getOrDefault("total", 0L);

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        return R.ok(stats);
    }

    /**
     * 获取当前用户的部门名称
     */
    private String getCurrentDeptName() {
        SysUser user = userService.getById(StpUtil.getLoginIdAsLong());
        if (user != null && user.getDeptId() != null) {
            SysDept dept = deptService.getById(user.getDeptId());
            if (dept != null) {
                return dept.getDeptName();
            }
        }
        return "";
    }

    /**
     * 解析 Excel 文件，提取线索列表
     * 列顺序: title, content, url, source_type, risk_level, summary
     */
    private List<BizRiskClue> parseExcel(MultipartFile file) throws Exception {
        List<BizRiskClue> clues = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            // 跳过表头行
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                BizRiskClue clue = new BizRiskClue();
                clue.setTitle(getCellStringValue(row, 0));
                clue.setContent(getCellStringValue(row, 1));
                clue.setUrl(getCellStringValue(row, 2));
                clue.setSourceType(getCellStringValue(row, 3));
                clue.setRiskLevel(getCellStringValue(row, 4));
                clue.setSummary(getCellStringValue(row, 5));

                // 跳过空行
                if (clue.getTitle() != null && !clue.getTitle().trim().isEmpty()) {
                    clues.add(clue);
                }
            }
        }

        return clues;
    }

    /**
     * 安全获取单元格字符串值
     */
    private String getCellStringValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) {
            return "";
        }
        DataFormatter dataFormatter = new DataFormatter();
        return dataFormatter.formatCellValue(cell);
    }
}
