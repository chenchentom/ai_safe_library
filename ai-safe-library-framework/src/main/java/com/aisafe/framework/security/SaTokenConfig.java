package com.aisafe.framework.security;

import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpLogic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sa-Token 核心配置
 *
 * 关键决策：
 * - 采用 JWT 模式：无状态，不依赖 Redis 存储登录态
 * - Token 存放在请求头 Authorization: Bearer xxx
 * - 所有需要登录的接口由 WebMvcConfig 中的拦截器保护
 */
@Configuration
public class SaTokenConfig {

    /**
     * 注入 Sa-Token JWT 实现
     * 替换默认的 Redis 存储模式，改为 JWT 自包含 Token
     */
    @Bean
    public StpLogic stpLogic() {
        return new StpLogicJwtForSimple();
    }

}
