package com.hermes.sdk.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hermes.sdk.config.HermesConfig;
import com.hermes.sdk.exception.*;
import com.hermes.sdk.logging.HermesLogger;
import com.hermes.sdk.logging.LogEvents;
import com.hermes.sdk.request.OpenAIRequest;
import okhttp3.*;
import org.slf4j.Logger;

import java.io.IOException;
import java.time.Duration;

/**
 * Hermes Java SDK 客户端
 * 
 * 线程安全，支持重试，异常细分，日志完善
 * 
 * 用法:
 *   HermesClient hermes = HermesClient.builder()
 *       .baseUrl("https://api.hermes.com")
 *       .maxRetries(3)
 *       .build();
 *   
 *   String result = hermes.chat("分析代码");
 *   String result = hermes.activateSkill("karpathy-principles", "重构 auth 模块");
 */
public class HermesClient {
    
    private static final Logger log = HermesLogger.get(HermesClient.class);
    
    private final HermesConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public HermesClient(Builder builder) {
        this.config = builder.build();
        this.httpClient = buildHttpClient(builder);
        this.objectMapper = new ObjectMapper();
    }
    
    // ========== Getters（供 Service 层使用）==========
    
    public HermesConfig getConfig() { return config; }
    public OkHttpClient getHttpClient() { return httpClient; }
    public ObjectMapper getObjectMapper() { return objectMapper; }
    public String getBaseUrl() { return config.getBaseUrl(); }
    
    private OkHttpClient buildHttpClient(Builder builder) {
        return new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(config.getConnectTimeout()))
            .readTimeout(Duration.ofSeconds(config.getReadTimeout()))
            .retryOnConnectionFailure(false)
            .build();
    }
    
    // ========== 聊天接口 ==========
    
    /**
     * 简单聊天
     */
    public String chat(String message) {
        validateNotEmpty(message, "消息内容不能为空");
        log.info("[{}] >>> chat() 请求: {}", LogEvents.HERMES_CHAT_REQUEST, maskContent(message));
        try {
            OpenAIRequest request = new OpenAIRequest(message);
            String response = doChat(request);
            log.info("[{}] <<< chat() 响应: {} chars", LogEvents.HERMES_CHAT_RESPONSE, response.length());
            return response;
        } catch (HermesException e) {
            log.error("[{}] <<< chat() 失败: errorCode={}, msg={}", 
                LogEvents.HERMES_CHAT_ERROR, e.getErrorCode(), e.getMessage());
            throw e;
        }
    }
    
    /**
     * 带系统提示的聊天
     */
    public String chatWithSystemPrompt(String systemPrompt, String message) {
        validateNotEmpty(message, "消息内容不能为空");
        log.info("[{}] >>> chatWithSystemPrompt() 请求: {}", LogEvents.HERMES_CHAT_REQUEST, maskContent(message));
        try {
            OpenAIRequest request = new OpenAIRequest(message, systemPrompt);
            String response = doChat(request);
            log.info("[{}] <<< chatWithSystemPrompt() 响应: {} chars", LogEvents.HERMES_CHAT_RESPONSE, response.length());
            return response;
        } catch (HermesException e) {
            log.error("[{}] <<< chatWithSystemPrompt() 失败: errorCode={}, msg={}", 
                LogEvents.HERMES_CHAT_ERROR, e.getErrorCode(), e.getMessage());
            throw e;
        }
    }
    
    /**
     * 激活 Skill（通过 system prompt）
     */
    public String activateSkill(String skillName, String userMessage) {
        validateNotEmpty(skillName, "Skill 名称不能为空");
        validateNotEmpty(userMessage, "用户消息不能为空");
        log.info("[{}] >>> activateSkill() skill={}, msg={}", LogEvents.HERMES_SKILL_ACTIVATE, skillName, maskContent(userMessage));
        try {
            OpenAIRequest request = OpenAIRequest.withSkill(skillName, userMessage);
            String response = doChat(request);
            log.info("[{}] <<< activateSkill() skill={} 成功, {} chars", LogEvents.HERMES_SKILL_ACTIVATE, skillName, response.length());
            return response;
        } catch (HermesException e) {
            log.error("[{}] <<< activateSkill() skill={} 失败: errorCode={}, msg={}", 
                LogEvents.HERMES_SKILL_ACTIVATE, skillName, e.getErrorCode(), e.getMessage());
            throw e;
        }
    }
    
    /**
     * 执行聊天请求（带重试）
     */
    private String doChat(OpenAIRequest request) {
        int attempts = 0;
        int maxRetries = config.getMaxRetries();
        
        while (true) {
            try {
                return executeChat(request);
            } catch (HermesNetworkException e) {
                attempts++;
                if (attempts >= maxRetries) {
                    log.error("[{}] <<< 重试次数用尽: attempt={}, msg={}", LogEvents.HERMES_CHAT_ERROR, attempts, e.getMessage());
                    throw e;
                }
                long delayMs = (long) Math.pow(2, attempts - 1) * 1000;
                log.warn("[{}] 重试: attempt={}/{}, delay={}ms, msg={}", 
                    LogEvents.HERMES_RETRY_NETWORK, attempts, maxRetries, delayMs, e.getMessage());
                try { Thread.sleep(delayMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
    }
    
    /**
     * 执行单次 HTTP 请求
     */
    private String executeChat(OpenAIRequest request) {
        String json;
        try {
            json = objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new HermesApiException("JSON_ENCODE", -1, e.getMessage());
        }
        
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request httpRequest = new Request.Builder()
            .url(config.getBaseUrl() + "/v1/chat/completions")
            .post(body)
            .build();
        
        try (Response resp = httpClient.newCall(httpRequest).execute()) {
            String responseBody = resp.body().string();
            
            if (!resp.isSuccessful()) {
                throw new HermesApiException("CHAT", resp.code(), responseBody);
            }
            
            JsonNode node = objectMapper.readTree(responseBody);
            JsonNode choices = node.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                if (message != null && message.has("content")) {
                    return message.get("content").asText();
                }
            }
            
            return responseBody;
            
        } catch (HermesApiException e) {
            throw e;
        } catch (IOException e) {
            throw new HermesNetworkException("executeChat", e);
        }
    }
    
    // ========== 健康检查 ==========
    
    /**
     * 健康检查
     */
    public boolean healthCheck() {
        log.info("[{}] 健康检查", LogEvents.HERMES_HEALTH_CHECK);
        try {
            Request request = new Request.Builder()
                .url(config.getBaseUrl() + "/health")
                .get()
                .build();
            
            try (Response resp = httpClient.newCall(request).execute()) {
                boolean healthy = resp.isSuccessful();
                log.info("[{}] 健康检查: {}", LogEvents.HERMES_HEALTH_CHECK, healthy);
                return healthy;
            }
        } catch (IOException e) {
            log.warn("[{}] 健康检查失败: {}", LogEvents.HERMES_HEALTH_CHECK, e.getMessage());
            return false;
        }
    }
    
    // ========== 会话支持 ==========
    
    /**
     * 创建普通会话
     */
    public ChatSession newSession() {
        return new ChatSession(this);
    }
    
    /**
     * 创建线程安全会话
     */
    public ThreadSafeChatSession newThreadSafeSession() {
        return new ThreadSafeChatSession(this);
    }
    
    // ========== 工具方法 ==========
    
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
    
    // ========== Builder ==========
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String baseUrl = "http://localhost:8080";
        private int connectTimeout = 30;
        private int readTimeout = 180;
        private int maxRetries = 3;
        
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
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
            return new HermesClient(this);
        }
    }
}