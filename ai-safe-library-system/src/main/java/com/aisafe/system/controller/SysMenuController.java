package com.aisafe.system.controller;

import com.aisafe.common.result.R;
import com.aisafe.system.service.SysPermissionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system/menu")
public class SysMenuController {

    private final SysPermissionService permissionService;

    public SysMenuController(SysPermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /** 完整菜单树（角色配置勾选用，需 system:role:list 或 admin） */
    @GetMapping("/tree")
    public R<List<Map<String, Object>>> tree() {
        return R.ok(permissionService.buildMenuTreeAll());
    }
}
