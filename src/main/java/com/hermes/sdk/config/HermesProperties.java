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
 *   - model: gpt-4
 *   - temperature: 0.7
 *   - max-tokens: 4096
 *   - connect-timeout: 30
 *   - read-timeout: 180
 *   - max-retries: 3
 *   - require-https: true
 */
@Data
@ConfigurationProperties(prefix = "hermes")
public class HermesProperties {
    
    /** Hermes Gateway 地址 */
    private String baseUrl = "http://localhost:8080";
    
    /** 默认模型 */
    private String model = "gpt-4";
    
    /** 连接超时（秒） */
    private int connectTimeout = 30;
    
    /** 读取超时（秒） */
    private int readTimeout = 180;
    
    /** 默认 temperature */
    private double temperature = 0.7;
    
    /** 默认最大 token */
    private int maxTokens = 4096;
}