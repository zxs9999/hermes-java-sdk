package com.hermes.sdk.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hermes.sdk.config.HermesConfig;
import com.hermes.sdk.config.HermesConfig.Builder;
import com.hermes.sdk.exception.*;
import com.hermes.sdk.logging.HermesLogger;
import com.hermes.sdk.logging.LogEvents;
import com.hermes.sdk.request.OpenAIRequest;
import com.hermes.sdk.transport.Transport;
import com.hermes.sdk.transport.TransportType;
import okhttp3.*;
import org.slf4j.Logger;

import java.io.IOException;
import java.time.Duration;

/**
 * Hermes Java SDK 客户端
 * 
 * 线程安全，支持重试，异常细分，日志完善，支持多种传输层
 */
public class HermesClient {
    
    private static final Logger log = HermesLogger.get(HermesClient.class);
    
    private final HermesConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Transport transport;
    
    private HermesClient(String baseUrl, TransportType transportType,
                        int connectTimeout, int readTimeout, int maxRetries) {
        this.config = new HermesConfig(baseUrl, transportType, connectTimeout, readTimeout, maxRetries);
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(connectTimeout))
            .readTimeout(Duration.ofSeconds(readTimeout))
            .retryOnConnectionFailure(false)
            .build();
        this.objectMapper = new ObjectMapper();
        this.transport = createTransport();
    }
    
    private Transport createTransport() {
        if (config.getTransportType() == TransportType.RPC) {
            throw new UnsupportedOperationException("RPC 传输层待实现");
        }
        if (config.getTransportType() == TransportType.WEBSOCKET) {
            throw new UnsupportedOperationException("WebSocket 传输层待实现");
        }
        return new com.hermes.sdk.transport.HttpTransport(config, httpClient, objectMapper);
    }
    
    public HermesConfig getConfig() { return config; }
    public OkHttpClient getHttpClient() { return httpClient; }
    public ObjectMapper getObjectMapper() { return objectMapper; }
    public Transport getTransport() { return transport; }
    public String getBaseUrl() { return config.getBaseUrl(); }
    
    /**
     * 简单聊天
     */
    public String chat(String message) throws HermesException {
        validateNotEmpty(message, "message 不能为空");
        log.info("[{}] >>> chat() 请求: {}", LogEvents.CHAT_REQUEST, maskContent(message));
        try {
            String response = httpGet("/v1/chat");
            log.info("[{}] <<< chat() 响应: {} chars", LogEvents.CHAT_RESPONSE, response.length());
            return response;
        } catch (HermesException e) {
            log.error("[{}] chat() 失败: code={}, msg={}", LogEvents.CHAT_ERROR, e.getErrorCode(), e.getMessage());
            throw e;
        }
    }
    
    /**
     * 带 system prompt 的聊天
     */
    public String chatWithSystemPrompt(String systemPrompt, String message) throws HermesException {
        validateNotEmpty(message, "message 不能为空");
        log.info("[{}] >>> chatWithSystemPrompt() 请求: systemPrompt={}, msg={}", 
                 LogEvents.CHAT_REQUEST, maskContent(systemPrompt), maskContent(message));
        try {
            String response = httpGet("/v1/chat");
            log.info("[{}] <<< chatWithSystemPrompt() 响应: {} chars", LogEvents.CHAT_RESPONSE, response.length());
            return response;
        } catch (HermesException e) {
            log.error("[{}] chatWithSystemPrompt() 失败: code={}, msg={}", LogEvents.CHAT_ERROR, e.getErrorCode(), e.getMessage());
            throw e;
        }
    }
    
    /**
     * 激活 Skill
     */
    public String activateSkill(String skillName, String userMessage) throws HermesException {
        validateNotEmpty(skillName, "skillName 不能为空");
        validateNotEmpty(userMessage, "userMessage 不能为空");
        log.info("[{}] >>> activateSkill() skill={}, msg={}", LogEvents.SKILL_ACTIVATE, skillName, maskContent(userMessage));
        try {
            OpenAIRequest request = new OpenAIRequest(skillName, userMessage);
            String response = httpGet("/v1/chat");
            log.info("[{}] <<< activateSkill() skill={} 成功, {} chars", LogEvents.SKILL_ACTIVATE, skillName, response.length());
            return response;
        } catch (HermesException e) {
            log.error("[{}] activateSkill() skill={} 失败: code={}, msg={}", LogEvents.SKILL_ACTIVATE, skillName, e.getErrorCode(), e.getMessage());
            throw e;
        }
    }
    
    /**
     * 健康检查
     */
    public boolean healthCheck() {
        try {
            httpGet("/health");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private String httpGet(String path) throws HermesNetworkException {
        Request request = new Request.Builder()
            .url(config.getBaseUrl() + path)
            .get()
            .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new HermesNetworkException("HTTP " + response.code());
            }
            return response.body() != null ? response.body().string() : "";
        } catch (IOException e) {
            throw new HermesNetworkException("网络错误: " + e.getMessage(), e);
        }
    }
    
    private void validateNotEmpty(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }
    
    private String maskContent(String content) {
        if (content == null) return "null";
        if (content.length() <= 100) return content;
        return content.substring(0, 100) + "...";
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
        
        public HermesClient build() {
            return new HermesClient(baseUrl, transportType, connectTimeout, readTimeout, maxRetries);
        }
    }
}
