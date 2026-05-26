package com.aisafe.system.controller;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.aisafe.common.result.R;
import com.aisafe.system.dto.LoginBody;
import com.aisafe.system.entity.SysUser;
import com.aisafe.system.service.LoginService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器 — 登录 / 登出 / 获取当前用户信息
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final LoginService loginService;

    public AuthController(LoginService loginService) {
        this.loginService = loginService;
    }

    /**
     * 用户登录
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public R<SaTokenInfo> login(@Valid @RequestBody LoginBody body) {
        SaTokenInfo tokenInfo = loginService.login(body);
        return R.ok("登录成功", tokenInfo);
    }

    /**
     * 用户登出
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public R<String> logout() {
        StpUtil.logout();
        return R.ok("退出成功");
    }

    /**
     * 获取当前登录用户信息
     * GET /api/auth/info
     * 请求头需携带: Authorization: Bearer {token}
     */
    @GetMapping("/info")
    public R<Map<String, Object>> info() {
        SysUser user = loginService.getCurrentUser();
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", user.getId());
        userInfo.put("userName", user.getUsername());
        userInfo.put("nickName", user.getNickname());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("roles", new String[]{});
        userInfo.put("permissions", new String[]{});
        return R.ok(userInfo);
    }

}
