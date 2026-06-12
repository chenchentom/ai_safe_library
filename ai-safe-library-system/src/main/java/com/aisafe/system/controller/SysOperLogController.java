package com.aisafe.system.controller;

import com.aisafe.common.enums.BusinessType;
import com.aisafe.common.result.R;
import com.aisafe.system.entity.SysOperLog;
import com.aisafe.system.mapper.SysOperLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system/oper-log")
public class SysOperLogController {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SysOperLogMapper operLogMapper;

    public SysOperLogController(SysOperLogMapper operLogMapper) {
        this.operLogMapper = operLogMapper;
    }

    @GetMapping("/list")
    public R<Map<String, Object>> list(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer businessType,
            @RequestParam(required = false) String operName,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String beginTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(title)) {
            wrapper.like(SysOperLog::getTitle, title);
        }
        if (businessType != null) {
            wrapper.eq(SysOperLog::getBusinessType, businessType);
        }
        if (StringUtils.hasText(operName)) {
            wrapper.like(SysOperLog::getOperName, operName);
        }
        if (status != null) {
            wrapper.eq(SysOperLog::getStatus, status);
        }
        LocalDateTime begin = parseDateTime(beginTime);
        LocalDateTime end = parseDateTime(endTime);
        if (begin != null) {
            wrapper.ge(SysOperLog::getOperTime, begin);
        }
        if (end != null) {
            wrapper.le(SysOperLog::getOperTime, end);
        }
        wrapper.orderByDesc(SysOperLog::getOperTime);

        Page<SysOperLog> page = operLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SysOperLog item : page.getRecords()) {
            rows.add(toMap(item));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("rows", rows);
        result.put("total", page.getTotal());
        return R.ok(result);
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        SysOperLog item = operLogMapper.selectById(id);
        if (item == null) {
            return R.fail("日志不存在");
        }
        return R.ok(toMap(item));
    }

    private Map<String, Object> toMap(SysOperLog item) {
        Map<String, Object> map = new HashMap<>();
        map.put("operId", item.getId());
        map.put("title", item.getTitle());
        map.put("businessType", item.getBusinessType());
        map.put("businessTypeLabel", BusinessType.labelOf(item.getBusinessType()));
        map.put("method", item.getMethod());
        map.put("requestMethod", item.getRequestMethod());
        map.put("operName", item.getOperName());
        map.put("deptName", item.getDeptName());
        map.put("operUrl", item.getOperUrl());
        map.put("operIp", item.getOperIp());
        map.put("operLocation", item.getOperLocation());
        map.put("operParam", item.getOperParam());
        map.put("jsonResult", item.getJsonResult());
        map.put("status", item.getStatus());
        map.put("errorMsg", item.getErrorMsg());
        if (item.getOperTime() != null) {
            map.put("operTime", item.getOperTime().format(FORMATTER));
        }
        map.put("costTime", item.getCostTime());
        return map;
    }

    private LocalDateTime parseDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), FORMATTER);
        } catch (Exception ignored) {
            return null;
        }
    }
}
