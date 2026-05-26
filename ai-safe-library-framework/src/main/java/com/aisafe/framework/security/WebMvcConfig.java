package com.aisafe.framework.security;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 *
 * 主要职责：
 * 1. 跨域配置 (CorsFilter，在 Filter 层处理，优先级高于 Sa-Token 拦截器)
 * 2. 注册 Sa-Token 路由拦截器
 * 3. 配置登录/匿名访问路径
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 跨域过滤器 —— 必须放在 Filter 层，确保在 Sa-Token 拦截器之前处理 OPTIONS 预检请求
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                // 拦截所有请求
                .addPathPatterns("/**")
                // 排除：登录接口 + 静态资源 + Swagger(预留)
                .excludePathPatterns(
                        "/auth/login",
                        "/auth/captcha",
                        "/static/**",
                        "/doc.html",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/swagger*/**"
                );
    }

}
