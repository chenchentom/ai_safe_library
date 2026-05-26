package com.aisafe.framework.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;

/**
 * Elasticsearch 客户端配置
 *
 * dev 环境：ES 使用自签名 HTTPS 证书，跳过 SSL 验证。
 * prod 环境：必须使用正式 CA 证书，不可跳过验证。
 */
@Configuration
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${spring.elasticsearch.uris}")
    private String uris;

    @Value("${spring.elasticsearch.username}")
    private String username;

    @Value("${spring.elasticsearch.password}")
    private String password;

    @Override
    public ClientConfiguration clientConfiguration() {
        return ClientConfiguration.builder()
                .connectedTo(parseHostAndPort(uris))
                .usingSsl(sslContext(), HostnameVerifierAllowAll())
                .withBasicAuth(username, password)
                .build();
    }

    /**
     * 解析 URIs 配置中的 host:port
     * 输入: https://localhost:9200 → 输出: localhost:9200
     */
    private String parseHostAndPort(String uris) {
        // 去掉协议前缀
        String cleaned = uris.replace("https://", "").replace("http://", "");
        // 去掉尾部斜杠
        if (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned;
    }

    /**
     * 创建信任所有证书的 SSLContext (仅开发环境！)
     */
    private SSLContext sslContext() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            return sslContext;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create SSL context for ES", e);
        }
    }

    /**
     * 跳过主机名验证 (仅开发环境！)
     */
    private HostnameVerifier HostnameVerifierAllowAll() {
        return (hostname, session) -> true;
    }

}
