package com.hermes.sdk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Hermes SDK 配置属性
 * 
 * application.yml 中配置：
 *   hermes:
 *     base-url: ${HERMES_BASE_URL:http://localhost:8080}
 * 
 * 其他参数使用 Builder 默认值，无需配置：
 *   - connect-timeout: 30
 *   - read-timeout: 180
 *   - max-retries: 3
 */
@Data
@ConfigurationProperties(prefix = "hermes")
public class HermesProperties {
    
    /** Hermes Gateway 地址 */
    private String baseUrl = "http://localhost:8080";
    
    /** 连接超时（秒） */
    private int connectTimeout = 30;
    
    /** 读取超时（秒） */
    private int readTimeout = 180;
    
    /** 最大重试次数 */
    private int maxRetries = 3;
}