package com.hermes.sdk.config;

import lombok.Data;

/**
 * Hermes SDK 配置
 */
@Data
public class HermesConfig {
    
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
    
    public HermesConfig() {}
    
    public HermesConfig(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }
}