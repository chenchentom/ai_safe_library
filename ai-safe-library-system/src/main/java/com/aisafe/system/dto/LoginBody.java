package com.aisafe.system.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求体
 */
public class LoginBody {

    /** 用户名 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 密码 (明文，后端 BCrypt 比对) */
    @NotBlank(message = "密码不能为空")
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

}
