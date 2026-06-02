package com.hermes.sdk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Hermes SDK 配置属性
 * 
 * application.yml 中配置：
 *   hermes:
 *     base-url: http://localhost:8080
 *     api-key: ${HERMES_API_KEY}  # 从环境变量读取，不写死
 *     model: gpt-4
 */
@Data
@ConfigurationProperties(prefix = "hermes")
public class HermesProperties {
    
    /** Hermes Gateway 地址 */
    private String baseUrl = "http://localhost:8080";
    
    /** API Key */
    private String apiKey;
    
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