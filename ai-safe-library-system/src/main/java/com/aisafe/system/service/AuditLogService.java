package com.aisafe.system.service;

import com.aisafe.common.enums.BusinessType;
import com.aisafe.system.dto.AuditLogContext;
import com.aisafe.system.entity.SysLoginInfo;
import com.aisafe.system.entity.SysOperLog;
import com.aisafe.system.entity.SysDept;
import com.aisafe.system.entity.SysUser;
import com.aisafe.system.util.ServletUtils;
import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 审计日志服务 — 同步捕获上下文，异步写入数据库
 */
@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
    private static final int MAX_PARAM_LENGTH = 4000;

    private final ISysUserService userService;
    private final ISysDeptService deptService;
    private final AuditLogAsyncWriter asyncWriter;
    private final ObjectMapper objectMapper;

    public AuditLogService(ISysUserService userService,
                           ISysDeptService deptService,
                           AuditLogAsyncWriter asyncWriter,
                           ObjectMapper objectMapper) {
        this.userService = userService;
        this.deptService = deptService;
        this.asyncWriter = asyncWriter;
        this.objectMapper = objectMapper;
    }

    public void recordOperSuccess(String title, BusinessType businessType, String method, Object operParam) {
        recordOper(title, businessType, method, operParam, 0, null);
    }

    public void recordOperFailure(String title, BusinessType businessType, String method,
                                  Object operParam, String errorMsg) {
        recordOper(title, businessType, method, operParam, 1, errorMsg);
    }

    public void recordOper(String title, BusinessType businessType, String method,
                           Object operParam, int status, String errorMsg) {
        AuditLogContext ctx = captureContext();
        SysOperLog entity = new SysOperLog();
        entity.setTitle(title);
        entity.setBusinessType(businessType != null ? businessType.getCode() : BusinessType.OTHER.getCode());
        entity.setMethod(method);
        entity.setRequestMethod(ctx.getRequestMethod());
        entity.setOperName(ctx.getOperName());
        entity.setDeptName(ctx.getDeptName());
        entity.setOperUrl(ctx.getOperUrl());
        entity.setOperIp(ctx.getOperIp());
        entity.setOperParam(toJson(operParam));
        entity.setStatus(status);
        entity.setErrorMsg(truncate(errorMsg, 2000));
        entity.setOperTime(LocalDateTime.now());
        asyncWriter.saveOperLog(entity);
    }

    public void recordLogin(String username, boolean success, String msg, HttpServletRequest request) {
        HttpServletRequest req = request != null ? request : ServletUtils.getRequest();
        String ip = ServletUtils.getClientIp(req);
        String userAgent = ServletUtils.getUserAgent(req);

        SysLoginInfo entity = new SysLoginInfo();
        entity.setUserName(username != null ? username : "");
        entity.setIpaddr(ip);
        entity.setBrowser(ServletUtils.parseBrowser(userAgent));
        entity.setOs(ServletUtils.parseOs(userAgent));
        entity.setStatus(success ? "0" : "1");
        entity.setMsg(truncate(msg, 255));
        entity.setLoginTime(LocalDateTime.now());
        asyncWriter.saveLoginInfo(entity);
    }

    public Map<String, Object> buildClueSnapshot(String id, String title, Integer auditStatus) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", id);
        snapshot.put("title", truncate(title, 200));
        snapshot.put("auditStatus", auditStatus);
        return snapshot;
    }

    private AuditLogContext captureContext() {
        AuditLogContext ctx = new AuditLogContext();
        HttpServletRequest request = ServletUtils.getRequest();
        ctx.setOperIp(ServletUtils.getClientIp(request));
        ctx.setUserAgent(ServletUtils.getUserAgent(request));
        if (request != null) {
            ctx.setOperUrl(request.getRequestURI());
            ctx.setRequestMethod(request.getMethod());
        }

        try {
            if (StpUtil.isLogin()) {
                SysUser user = userService.getById(StpUtil.getLoginIdAsLong());
                if (user != null) {
                    String displayName = StringUtils.hasText(user.getNickname())
                            ? user.getNickname() : user.getUsername();
                    ctx.setOperName(displayName);
                    if (user.getDeptId() != null) {
                        SysDept dept = deptService.getById(user.getDeptId());
                        if (dept != null) {
                            ctx.setDeptName(dept.getDeptName());
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("capture oper context skipped: {}", ex.getMessage());
        }

        if (!StringUtils.hasText(ctx.getOperName())) {
            ctx.setOperName("system");
        }
        return ctx;
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            String json = value instanceof String str ? str : objectMapper.writeValueAsString(value);
            return truncate(json, MAX_PARAM_LENGTH);
        } catch (JsonProcessingException ex) {
            return truncate(String.valueOf(value), MAX_PARAM_LENGTH);
        }
    }

    private static String truncate(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen);
    }

}
