package com.aisafe.system.service;

import com.aisafe.system.entity.SysLoginInfo;
import com.aisafe.system.entity.SysOperLog;
import com.aisafe.system.mapper.SysLoginInfoMapper;
import com.aisafe.system.mapper.SysOperLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AuditLogAsyncWriter {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAsyncWriter.class);

    private final SysOperLogMapper operLogMapper;
    private final SysLoginInfoMapper loginInfoMapper;

    public AuditLogAsyncWriter(SysOperLogMapper operLogMapper, SysLoginInfoMapper loginInfoMapper) {
        this.operLogMapper = operLogMapper;
        this.loginInfoMapper = loginInfoMapper;
    }

    @Async("auditLogExecutor")
    public void saveOperLog(SysOperLog entity) {
        try {
            operLogMapper.insert(entity);
        } catch (Exception ex) {
            log.warn("操作日志写入失败 title={}: {}", entity.getTitle(), ex.getMessage());
        }
    }

    @Async("auditLogExecutor")
    public void saveLoginInfo(SysLoginInfo entity) {
        try {
            loginInfoMapper.insert(entity);
        } catch (Exception ex) {
            log.warn("登录日志写入失败 user={}: {}", entity.getUserName(), ex.getMessage());
        }
    }
}
