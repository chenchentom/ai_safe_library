package com.aisafe.system.service;

import com.aisafe.system.dto.RoleSaveRequest;
import com.aisafe.system.entity.SysMenu;
import com.aisafe.system.entity.SysRole;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 权限解析：用户角色 = 用户直接角色 ∪ 部门角色；菜单与权限标识由角色汇总。
 */
public interface SysPermissionService {

    Set<Long> resolveRoleIds(Long userId, Long deptId);

    List<String> getRoleKeys(Long userId, Long deptId);

    List<String> getPermissionKeys(Long userId, Long deptId);

    List<SysMenu> getMenusForUser(Long userId, Long deptId);

    List<Map<String, Object>> buildMenuTreeForUser(Long userId, Long deptId);

    List<Map<String, Object>> buildMenuTreeAll();

    List<Long> getMenuIdsByRoleId(Long roleId);

    List<Long> getRoleIdsByUserId(Long userId);

    List<Long> getRoleIdsByDeptId(Long deptId);

    void assignUserRoles(Long userId, List<Long> roleIds);

    void assignDeptRoles(Long deptId, List<Long> roleIds);

    void assignRoleMenus(Long roleId, List<Long> menuIds);

    List<SysRole> listRoles(String roleName, String status);

    SysRole getRoleById(Long roleId);

    void saveRole(RoleSaveRequest request);

    void updateRole(RoleSaveRequest request);

    void deleteRole(Long roleId);

    boolean isSuperAdmin(Long userId, Long deptId);
}
