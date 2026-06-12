package com.aisafe.system.controller;

import com.aisafe.common.result.R;
import com.aisafe.system.entity.SysLoginInfo;
import com.aisafe.system.mapper.SysLoginInfoMapper;
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
@RequestMapping("/system/login-info")
public class SysLoginInfoController {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SysLoginInfoMapper loginInfoMapper;

    public SysLoginInfoController(SysLoginInfoMapper loginInfoMapper) {
        this.loginInfoMapper = loginInfoMapper;
    }

    @GetMapping("/list")
    public R<Map<String, Object>> list(
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String beginTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        LambdaQueryWrapper<SysLoginInfo> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(userName)) {
            wrapper.like(SysLoginInfo::getUserName, userName);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(SysLoginInfo::getStatus, status);
        }
        LocalDateTime begin = parseDateTime(beginTime);
        LocalDateTime end = parseDateTime(endTime);
        if (begin != null) {
            wrapper.ge(SysLoginInfo::getLoginTime, begin);
        }
        if (end != null) {
            wrapper.le(SysLoginInfo::getLoginTime, end);
        }
        wrapper.orderByDesc(SysLoginInfo::getLoginTime);

        Page<SysLoginInfo> page = loginInfoMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SysLoginInfo item : page.getRecords()) {
            rows.add(toMap(item));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("rows", rows);
        result.put("total", page.getTotal());
        return R.ok(result);
    }

    private Map<String, Object> toMap(SysLoginInfo item) {
        Map<String, Object> map = new HashMap<>();
        map.put("infoId", item.getId());
        map.put("userName", item.getUserName());
        map.put("ipaddr", item.getIpaddr());
        map.put("loginLocation", item.getLoginLocation());
        map.put("browser", item.getBrowser());
        map.put("os", item.getOs());
        map.put("status", item.getStatus());
        map.put("msg", item.getMsg());
        if (item.getLoginTime() != null) {
            map.put("loginTime", item.getLoginTime().format(FORMATTER));
        }
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
