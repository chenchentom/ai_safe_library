package com.aisafe.business.support;

import com.aisafe.common.exception.BusinessException;
import com.aisafe.system.entity.SysDept;
import com.aisafe.system.entity.SysUser;
import com.aisafe.system.service.ISysDeptService;
import com.aisafe.system.service.SysPermissionService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 报送模块部门解析与隔离
 */
@Component
public class ReportDeptSupport {

    private final ISysDeptService deptService;
    private final SysPermissionService permissionService;

    public ReportDeptSupport(ISysDeptService deptService, SysPermissionService permissionService) {
        this.deptService = deptService;
        this.permissionService = permissionService;
    }

    public String resolveDeptName(SysUser user) {
        if (user == null || user.getDeptId() == null) {
            return "";
        }
        SysDept dept = deptService.getById(user.getDeptId());
        return dept != null && dept.getDeptName() != null ? dept.getDeptName() : "";
    }

    /**
     * 非超管必须有部门；超管无部门时返回空串（表示不按部门过滤）
     */
    public String requireReportUnit(SysUser user) {
        String deptName = resolveDeptName(user);
        if (StringUtils.hasText(deptName)) {
            return deptName;
        }
        if (user != null && permissionService.isSuperAdmin(user.getId(), user.getDeptId())) {
            return "";
        }
        throw new BusinessException("当前用户未分配部门，无法访问报送数据");
    }

    public boolean isSuperAdmin(SysUser user) {
        return user != null && permissionService.isSuperAdmin(user.getId(), user.getDeptId());
    }
}
