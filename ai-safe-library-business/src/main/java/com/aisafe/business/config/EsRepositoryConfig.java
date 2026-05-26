package com.aisafe.business.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * ES Repository 扫描配置
 *
 * 必须显式声明 @EnableElasticsearchRepositories，因为 classpath 中同时存在
 * Spring Data Redis 和 Spring Data Elasticsearch，Spring Data 进入严格模式，
 * 不会自动扫描 ES Repository。
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.aisafe.business.repository")
public class EsRepositoryConfig {
}
