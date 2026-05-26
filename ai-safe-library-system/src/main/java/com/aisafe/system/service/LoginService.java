package com.aisafe.system.service;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.aisafe.common.exception.BusinessException;
import com.aisafe.system.dto.LoginBody;
import com.aisafe.system.entity.SysUser;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 登录鉴权服务
 *
 * 流程：
 * 1. 根据用户名查询用户
 * 2. BCrypt 密码比对
 * 3. 检查用户状态
 * 4. 执行 Sa-Token 登录
 * 5. 返回 Token 信息
 */
@Service
public class LoginService {

    private final ISysUserService userService;

    public LoginService(ISysUserService userService) {
        this.userService = userService;
    }

    /**
     * 用户登录
     * @param body 登录请求 (用户名 + 密码)
     * @return Sa-Token 登录信息 (包含 Token 值)
     */
    public SaTokenInfo login(LoginBody body) {
        // 1. 查用户
        SysUser user = userService.getByUsername(body.getUsername());
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        // 2. 验证密码 (BCrypt)
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (!encoder.matches(body.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        // 3. 检查用户状态
        if (!"0".equals(user.getStatus())) {
            throw new BusinessException("账号已被停用，请联系管理员");
        }

        // 4. 执行登录 (loginId 使用 userId，便于后续关联)
        StpUtil.login(user.getId());

        // 5. 返回 Token
        return StpUtil.getTokenInfo();
    }

    /**
     * 获取当前登录用户信息
     */
    public SysUser getCurrentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

}
