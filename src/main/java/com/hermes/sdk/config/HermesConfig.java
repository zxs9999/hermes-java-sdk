package com.hermes.sdk.config;

import com.hermes.sdk.transport.TransportType;

/**
 * Hermes SDK 配置
 * 
 * 线程安全：所有字段不可变（final + 无 setter）
 */
@Getter
public class HermesConfig {
    
    /** Hermes Gateway 地址 */
    private final String baseUrl;
    
    /** 传输类型 */
    private final TransportType transportType;
    
    /** 连接超时（秒） */
    private final int connectTimeout;
    
    /** 读取超时（秒） */
    private final int readTimeout;
    
    /** 最大重试次数 */
    private final int maxRetries;
    
    private HermesConfig(Builder builder) {
        this.baseUrl = builder.baseUrl;
        this.transportType = builder.transportType;
        this.connectTimeout = builder.connectTimeout;
        this.readTimeout = builder.readTimeout;
        this.maxRetries = builder.maxRetries;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String baseUrl = "http://localhost:8080";
        private TransportType transportType = TransportType.HTTP;
        private int connectTimeout = 30;
        private int readTimeout = 180;
        private int maxRetries = 3;
        
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }
        
        public Builder transportType(TransportType transportType) {
            this.transportType = transportType;
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
        
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }
        
        public HermesConfig build() {
            return new HermesConfig(this);
        }
    }
}