package com.hermes.sdk.config;

import lombok.Getter;
import lombok.Setter;

/**
 * Hermes SDK 配置
 * 
 * 线程安全：所有字段不可变（final + 无 setter）
 */
@Getter
public class HermesConfig {
    
    /** Hermes Gateway 地址 */
    private final String baseUrl;
    
    /** API Key */
    private final String apiKey;
    
    /** 默认模型 */
    private final String model;
    
    /** 连接超时（秒） */
    private final int connectTimeout;
    
    /** 读取超时（秒） */
    private final int readTimeout;
    
    /** 默认 temperature */
    private final double temperature;
    
    /** 默认最大 token */
    private final int maxTokens;
    
    /** 最大重试次数 */
    private final int maxRetries;
    
    /** 是否启用 HTTPS */
    private final boolean requireHttps;
    
    private HermesConfig(Builder builder) {
        this.baseUrl = builder.baseUrl;
        this.apiKey = builder.apiKey;
        this.model = builder.model;
        this.connectTimeout = builder.connectTimeout;
        this.readTimeout = builder.readTimeout;
        this.temperature = builder.temperature;
        this.maxTokens = builder.maxTokens;
        this.maxRetries = builder.maxRetries;
        this.requireHttps = builder.requireHttps;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static HermesConfig defaults(String baseUrl, String apiKey) {
        return builder().baseUrl(baseUrl).apiKey(apiKey).build();
    }
    
    /**
     * Builder 模式
     */
    public static class Builder {
        private String baseUrl = "http://localhost:8080";
        private String apiKey;
        private String model = "gpt-4";
        private int connectTimeout = 30;
        private int readTimeout = 180;
        private double temperature = 0.7;
        private int maxTokens = 4096;
        private int maxRetries = 3;
        private boolean requireHttps = true;
        
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }
        
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }
        
        public Builder model(String model) {
            this.model = model;
            return this;
        }
        
        public Builder connectTimeout(int seconds) {
            this.connectTimeout = seconds;
            return this;
        }
        
        public Builder readTimeout(int seconds) {
            this.readTimeout = seconds;
            return this;
        }
        
        public Builder temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }
        
        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }
        
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }
        
        public Builder requireHttps(boolean require) {
            this.requireHttps = require;
            return this;
        }
        
        public HermesConfig build() {
            if (requireHttps && !baseUrl.startsWith("https://")) {
                throw new SecurityException("Hermes SDK 要求使用 HTTPS，请配置 https:// 前缀");
            }
            return new HermesConfig(this);
        }
    }
}