package com.aisafe.framework.security;

import cn.dev33.satoken.stp.StpInterface;

import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 权限/角色加载器
 *
 * 每次鉴权时调用，从数据库中加载当前用户的角色和权限列表。
 * 阶段一：返回空列表，后续实现数据库查询。
 */
/** @deprecated 由 system 模块 {@code SysStpInterfaceImpl} 提供实现 */
public class StpInterfaceImpl implements StpInterface {

    /**
     * 返回当前用户拥有的权限标识列表 (如: system:user:add)
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 阶段一 TODO: 从 sys_role_menu 关联查询权限标识
        return Collections.emptyList();
    }

    /**
     * 返回当前用户拥有的角色标识列表 (如: admin)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 阶段一 TODO: 从 sys_user_role 关联查询角色
        return Collections.emptyList();
    }

}
