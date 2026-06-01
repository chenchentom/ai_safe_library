package com.aisafe.system.service.impl;

import com.aisafe.common.exception.BusinessException;
import com.aisafe.system.dto.RoleSaveRequest;
import com.aisafe.system.entity.*;
import com.aisafe.system.mapper.*;
import com.aisafe.system.service.SysPermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SysPermissionServiceImpl implements SysPermissionService {

    private static final String SUPER_ADMIN_ROLE_KEY = "admin";

    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysDeptRoleMapper deptRoleMapper;

    public SysPermissionServiceImpl(SysRoleMapper roleMapper,
                                    SysMenuMapper menuMapper,
                                    SysRoleMenuMapper roleMenuMapper,
                                    SysUserRoleMapper userRoleMapper,
                                    SysDeptRoleMapper deptRoleMapper) {
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.userRoleMapper = userRoleMapper;
        this.deptRoleMapper = deptRoleMapper;
    }

    @Override
    public Set<Long> resolveRoleIds(Long userId, Long deptId) {
        Set<Long> roleIds = new LinkedHashSet<>();
        if (userId != null) {
            roleIds.addAll(getRoleIdsByUserId(userId));
        }
        if (deptId != null) {
            roleIds.addAll(getRoleIdsByDeptId(deptId));
        }
        return roleIds;
    }

    @Override
    public List<String> getRoleKeys(Long userId, Long deptId) {
        Set<Long> roleIds = resolveRoleIds(userId, deptId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
        return roles.stream()
                .filter(r -> "0".equals(r.getStatus()))
                .map(SysRole::getRoleKey)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getPermissionKeys(Long userId, Long deptId) {
        List<SysMenu> menus = getMenusForUser(userId, deptId);
        return menus.stream()
                .map(SysMenu::getPerms)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<SysMenu> getMenusForUser(Long userId, Long deptId) {
        if (isSuperAdmin(userId, deptId)) {
            return listActiveMenus();
        }
        Set<Long> roleIds = resolveRoleIds(userId, deptId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysRoleMenu> roleMenus = roleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, roleIds));
        Set<Long> menuIds = roleMenus.stream()
                .map(SysRoleMenu::getMenuId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (menuIds.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> expandedIds = expandWithAncestors(menuIds);
        List<SysMenu> menus = menuMapper.selectBatchIds(expandedIds);
        return menus.stream()
                .filter(this::isMenuVisible)
                .sorted(Comparator.comparing(m -> m.getOrderNum() == null ? 0 : m.getOrderNum()))
                .collect(Collectors.toList());
    }

    private Set<Long> expandWithAncestors(Set<Long> menuIds) {
        Set<Long> expanded = new LinkedHashSet<>(menuIds);
        List<SysMenu> all = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getStatus, "0"));
        Map<Long, SysMenu> byId = all.stream().collect(Collectors.toMap(SysMenu::getId, m -> m, (a, b) -> a));
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Long id : new ArrayList<>(expanded)) {
                SysMenu menu = byId.get(id);
                if (menu != null && menu.getParentId() != null && menu.getParentId() > 0) {
                    if (expanded.add(menu.getParentId())) {
                        changed = true;
                    }
                }
            }
        }
        return expanded;
    }

    @Override
    public List<Map<String, Object>> buildMenuTreeForUser(Long userId, Long deptId) {
        List<SysMenu> menus = getMenusForUser(userId, deptId);
        return buildMenuTree(menus);
    }

    @Override
    public List<Map<String, Object>> buildMenuTreeAll() {
        return buildMenuTree(listPlatformMenus());
    }

    /** 角色配置用：仅展示本平台菜单（id>=1000） */
    private List<SysMenu> listPlatformMenus() {
        return listActiveMenus();
    }

    @Override
    public List<Long> getMenuIdsByRoleId(Long roleId) {
        return roleMenuMapper.selectList(
                        new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId))
                .stream()
                .map(SysRoleMenu::getMenuId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getRoleIdsByUserId(Long userId) {
        return userRoleMapper.selectList(
                        new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getRoleIdsByDeptId(Long deptId) {
        return deptRoleMapper.selectList(
                        new LambdaQueryWrapper<SysDeptRole>().eq(SysDeptRole::getDeptId, deptId))
                .stream()
                .map(SysDeptRole::getRoleId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignUserRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        for (Long roleId : roleIds) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignDeptRoles(Long deptId, List<Long> roleIds) {
        deptRoleMapper.delete(new LambdaQueryWrapper<SysDeptRole>().eq(SysDeptRole::getDeptId, deptId));
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        for (Long roleId : roleIds) {
            SysDeptRole dr = new SysDeptRole();
            dr.setDeptId(deptId);
            dr.setRoleId(roleId);
            deptRoleMapper.insert(dr);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoleMenus(Long roleId, List<Long> menuIds) {
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        for (Long menuId : menuIds) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            roleMenuMapper.insert(rm);
        }
    }

    @Override
    public List<SysRole> listRoles(String roleName, String status) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(roleName)) {
            wrapper.like(SysRole::getRoleName, roleName);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(SysRole::getStatus, status);
        }
        wrapper.orderByAsc(SysRole::getRoleSort);
        return roleMapper.selectList(wrapper);
    }

    @Override
    public SysRole getRoleById(Long roleId) {
        return roleMapper.selectById(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRole(RoleSaveRequest request) {
        SysRole role = new SysRole();
        role.setRoleName(request.getRoleName());
        role.setRoleKey(request.getRoleKey());
        role.setRoleSort(request.getRoleSort() != null ? request.getRoleSort() : 0);
        role.setDataScope(StringUtils.hasText(request.getDataScope()) ? request.getDataScope() : "5");
        role.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "0");
        role.setRemark(request.getRemark());
        roleMapper.insert(role);
        assignRoleMenus(role.getId(), request.getMenuIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(RoleSaveRequest request) {
        if (request.getRoleId() == null) {
            throw new BusinessException("角色ID不能为空");
        }
        SysRole role = roleMapper.selectById(request.getRoleId());
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        role.setRoleName(request.getRoleName());
        role.setRoleKey(request.getRoleKey());
        if (request.getRoleSort() != null) {
            role.setRoleSort(request.getRoleSort());
        }
        if (StringUtils.hasText(request.getDataScope())) {
            role.setDataScope(request.getDataScope());
        }
        if (StringUtils.hasText(request.getStatus())) {
            role.setStatus(request.getStatus());
        }
        role.setRemark(request.getRemark());
        roleMapper.updateById(role);
        if (request.getMenuIds() != null) {
            assignRoleMenus(role.getId(), request.getMenuIds());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long roleId) {
        roleMapper.deleteById(roleId);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, roleId));
        deptRoleMapper.delete(new LambdaQueryWrapper<SysDeptRole>().eq(SysDeptRole::getRoleId, roleId));
    }

    @Override
    public boolean isSuperAdmin(Long userId, Long deptId) {
        return getRoleKeys(userId, deptId).contains(SUPER_ADMIN_ROLE_KEY);
    }

    private List<SysMenu> listActiveMenus() {
        return menuMapper.selectList(
                        new LambdaQueryWrapper<SysMenu>()
                                .eq(SysMenu::getStatus, "0")
                                .ge(SysMenu::getId, 1000L)
                                .orderByAsc(SysMenu::getOrderNum))
                .stream()
                .filter(this::isMenuVisible)
                .collect(Collectors.toList());
    }

    private boolean isMenuVisible(SysMenu menu) {
        return menu != null
                && "0".equals(menu.getStatus())
                && !"1".equals(menu.getVisible())
                && ("M".equals(menu.getMenuType()) || "C".equals(menu.getMenuType()));
    }

    private List<Map<String, Object>> buildMenuTree(List<SysMenu> menus) {
        Map<Long, Map<String, Object>> nodeMap = new LinkedHashMap<>();
        for (SysMenu menu : menus) {
            nodeMap.put(menu.getId(), toMenuNode(menu));
        }
        List<Map<String, Object>> roots = new ArrayList<>();
        for (SysMenu menu : menus) {
            Map<String, Object> node = nodeMap.get(menu.getId());
            Long parentId = menu.getParentId();
            if (parentId == null || parentId == 0L || !nodeMap.containsKey(parentId)) {
                roots.add(node);
            } else {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children =
                        (List<Map<String, Object>>) nodeMap.get(parentId).computeIfAbsent("children", k -> new ArrayList<>());
                children.add(node);
            }
        }
        sortMenuTree(roots);
        return roots;
    }

    @SuppressWarnings("unchecked")
    private void sortMenuTree(List<Map<String, Object>> nodes) {
        nodes.sort(Comparator.comparingInt(n -> (Integer) n.getOrDefault("orderNum", 0)));
        for (Map<String, Object> node : nodes) {
            List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("children");
            if (children != null && !children.isEmpty()) {
                sortMenuTree(children);
            }
        }
    }

    private Map<String, Object> toMenuNode(SysMenu menu) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("menuId", menu.getId());
        node.put("parentId", menu.getParentId());
        node.put("menuName", menu.getMenuName());
        node.put("menuType", menu.getMenuType());
        node.put("path", menu.getPath());
        node.put("component", menu.getComponent());
        node.put("perms", menu.getPerms());
        node.put("icon", menu.getIcon());
        node.put("orderNum", menu.getOrderNum() != null ? menu.getOrderNum() : 0);
        node.put("children", new ArrayList<>());
        return node;
    }
}
