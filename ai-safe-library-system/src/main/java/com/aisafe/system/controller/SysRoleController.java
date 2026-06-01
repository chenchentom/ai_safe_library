package com.aisafe.system.controller;

import com.aisafe.common.result.R;
import com.aisafe.system.dto.RoleSaveRequest;
import com.aisafe.system.entity.SysRole;
import com.aisafe.system.service.SysPermissionService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/system/role")
public class SysRoleController {

    private final SysPermissionService permissionService;

    public SysRoleController(SysPermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping("/list")
    public R<List<Map<String, Object>>> list(
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String status) {
        List<SysRole> roles = permissionService.listRoles(roleName, status);
        List<Map<String, Object>> rows = roles.stream().map(this::toRoleMap).collect(Collectors.toList());
        return R.ok(rows);
    }

    @GetMapping("/{roleId}")
    public R<Map<String, Object>> get(@PathVariable Long roleId) {
        SysRole role = permissionService.getRoleById(roleId);
        if (role == null) {
            return R.fail("角色不存在");
        }
        Map<String, Object> data = toRoleMap(role);
        data.put("menuIds", permissionService.getMenuIdsByRoleId(roleId));
        return R.ok(data);
    }

    @PostMapping
    public R<String> add(@RequestBody RoleSaveRequest request) {
        permissionService.saveRole(request);
        return R.ok("新增成功");
    }

    @PutMapping
    public R<String> update(@RequestBody RoleSaveRequest request) {
        permissionService.updateRole(request);
        return R.ok("修改成功");
    }

    @DeleteMapping("/{roleId}")
    public R<String> delete(@PathVariable Long roleId) {
        permissionService.deleteRole(roleId);
        return R.ok("删除成功");
    }

    private Map<String, Object> toRoleMap(SysRole role) {
        Map<String, Object> map = new HashMap<>();
        map.put("roleId", role.getId());
        map.put("roleName", role.getRoleName());
        map.put("roleKey", role.getRoleKey());
        map.put("roleSort", role.getRoleSort());
        map.put("dataScope", role.getDataScope());
        map.put("status", role.getStatus());
        map.put("remark", role.getRemark());
        return map;
    }
}
