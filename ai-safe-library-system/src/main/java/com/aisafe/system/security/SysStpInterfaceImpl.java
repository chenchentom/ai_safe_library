package com.aisafe.system.security;

import cn.dev33.satoken.stp.StpInterface;
import com.aisafe.system.entity.SysUser;
import com.aisafe.system.service.ISysUserService;
import com.aisafe.system.service.SysPermissionService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SysStpInterfaceImpl implements StpInterface {

    private final SysPermissionService permissionService;
    private final ISysUserService userService;

    public SysStpInterfaceImpl(SysPermissionService permissionService, ISysUserService userService) {
        this.permissionService = permissionService;
        this.userService = userService;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = Long.valueOf(loginId.toString());
        SysUser user = userService.getById(userId);
        Long deptId = user != null ? user.getDeptId() : null;
        return permissionService.getPermissionKeys(userId, deptId);
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Long.valueOf(loginId.toString());
        SysUser user = userService.getById(userId);
        Long deptId = user != null ? user.getDeptId() : null;
        return permissionService.getRoleKeys(userId, deptId);
    }
}
