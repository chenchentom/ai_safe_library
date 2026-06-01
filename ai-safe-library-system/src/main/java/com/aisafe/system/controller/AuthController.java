package com.aisafe.system.controller;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.aisafe.common.result.R;
import com.aisafe.system.dto.LoginBody;
import com.aisafe.system.entity.SysDept;
import com.aisafe.system.entity.SysUser;
import com.aisafe.system.service.ISysDeptService;
import com.aisafe.system.service.LoginService;
import com.aisafe.system.service.SysPermissionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final LoginService loginService;
    private final ISysDeptService deptService;
    private final SysPermissionService permissionService;

    public AuthController(LoginService loginService,
                          ISysDeptService deptService,
                          SysPermissionService permissionService) {
        this.loginService = loginService;
        this.deptService = deptService;
        this.permissionService = permissionService;
    }

    @PostMapping("/login")
    public R<SaTokenInfo> login(@Valid @RequestBody LoginBody body) {
        SaTokenInfo tokenInfo = loginService.login(body);
        return R.ok("登录成功", tokenInfo);
    }

    @PostMapping("/logout")
    public R<String> logout() {
        StpUtil.logout();
        return R.ok("退出成功");
    }

    @GetMapping("/info")
    public R<Map<String, Object>> info() {
        SysUser user = loginService.getCurrentUser();
        return R.ok(buildUserInfo(user));
    }

    @GetMapping("/menus")
    public R<List<Map<String, Object>>> menus() {
        StpUtil.checkLogin();
        SysUser user = loginService.getCurrentUser();
        return R.ok(permissionService.buildMenuTreeForUser(user.getId(), user.getDeptId()));
    }

    private Map<String, Object> buildUserInfo(SysUser user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", user.getId());
        userInfo.put("userName", user.getUsername());
        userInfo.put("nickName", user.getNickname());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("deptId", user.getDeptId());
        if (user.getDeptId() != null) {
            SysDept dept = deptService.getById(user.getDeptId());
            if (dept != null) {
                userInfo.put("deptName", dept.getDeptName());
            }
        }
        userInfo.put("roles", permissionService.getRoleKeys(user.getId(), user.getDeptId()));
        userInfo.put("permissions", permissionService.getPermissionKeys(user.getId(), user.getDeptId()));
        userInfo.put("menus", permissionService.buildMenuTreeForUser(user.getId(), user.getDeptId()));
        userInfo.put("superAdmin", permissionService.isSuperAdmin(user.getId(), user.getDeptId()));
        return userInfo;
    }
}
