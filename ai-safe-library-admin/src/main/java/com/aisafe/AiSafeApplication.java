package com.aisafe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI安全事件库 — 应用入口
 *
 * @SpringBootApplication 自动扫描 com.aisafe 包下所有组件
 * 包括 common、framework、system、business 四个子模块
 */
@SpringBootApplication
@EnableScheduling
public class AiSafeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiSafeApplication.class, args);
        System.out.println("""

                ╔══════════════════════════════════════════╗
                ║     AI安全事件库 v1.0.0 启动成功          ║
                ║     访问: http://localhost:8080           ║
                ╚══════════════════════════════════════════╝
                """);
    }

}
