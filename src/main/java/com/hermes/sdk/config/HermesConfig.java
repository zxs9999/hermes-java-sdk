package com.hermes.sdk.config;

import com.hermes.sdk.transport.TransportType;

/**
 * Hermes SDK 配置
 */
public class HermesConfig {
    
    public final String baseUrl;
    public final TransportType transportType;
    public final int connectTimeout;
    public final int readTimeout;
    public final int maxRetries;
    
    public HermesConfig(String baseUrl, TransportType transportType,
                 int connectTimeout, int readTimeout, int maxRetries) {
        this.baseUrl = baseUrl;
        this.transportType = transportType;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.maxRetries = maxRetries;
    }
    
    public String getBaseUrl() { return baseUrl; }
    public TransportType getTransportType() { return transportType; }
    public int getConnectTimeout() { return connectTimeout; }
    public int getReadTimeout() { return readTimeout; }
    public int getMaxRetries() { return maxRetries; }
    
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private String baseUrl = "http://localhost:8080";
        private TransportType transportType = TransportType.HTTP;
        private int connectTimeout = 30;
        private int readTimeout = 180;
        private int maxRetries = 3;
        
        public Builder baseUrl(String v) { this.baseUrl = v; return this; }
        public Builder transportType(TransportType v) { this.transportType = v; return this; }
        public Builder connectTimeout(int v) { this.connectTimeout = v; return this; }
        public Builder readTimeout(int v) { this.readTimeout = v; return this; }
        public Builder maxRetries(int v) { this.maxRetries = v; return this; }
        
        public HermesConfig build() {
            return new HermesConfig(baseUrl, transportType, connectTimeout, readTimeout, maxRetries);
        }
    }
}
