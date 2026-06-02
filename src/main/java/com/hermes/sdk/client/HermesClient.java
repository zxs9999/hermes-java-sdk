package com.hermes.sdk.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hermes.sdk.config.HermesConfig;
import com.hermes.sdk.exception.*;
import com.hermes.sdk.request.ChatRequest;
import com.hermes.sdk.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Hermes Java SDK 客户端
 * 
 * 线程安全，支持重试，异常细分
 * 
 * 用法:
 *   HermesClient hermes = new HermesClient.Builder()
 *       .baseUrl("https://api.hermes.com")
 *       .apiKey(System.getenv("HERMES_API_KEY"))
 *       .maxRetries(3)
 *       .build();
 *   
 *   String result = hermes.chat("分析代码");
 *   String result = hermes.activateSkill("karpathy-principles", "重构 auth 模块");
 */
@Slf4j
public class HermesClient {
    
    private final HermesConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public HermesClient(Builder builder) {
        this.config = builder.build();
        this.httpClient = buildHttpClient(builder);
        this.objectMapper = new ObjectMapper();
    }
    
    private OkHttpClient buildHttpClient(Builder builder) {
        return new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(config.getConnectTimeout()))
            .readTimeout(Duration.ofSeconds(config.getReadTimeout()))
            .retryOnConnectionFailure(false) // 我们自己处理重试
            .build();
    }
    
    /**
     * 简单聊天
     */
    public String chat(String message) {
        validateNotEmpty(message, "消息内容不能为空");
        return chatWithSystemPrompt(null, message);
    }
    
    /**
     * 带 System Prompt 的聊天
     */
    public String chatWithSystemPrompt(String systemPrompt, String userMessage) {
        validateNotEmpty(userMessage, "用户消息不能为空");
        
        ChatRequest request = new ChatRequest();
        request.setModel(config.getModel());
        request.setTemperature(config.getTemperature());
        request.setMaxTokens(config.getMaxTokens());
        
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            request.addSystemMessage(systemPrompt);
        }
        request.addUserMessage(userMessage);
        
        String json = toJson(request);
        return executeWithRetry(request, json, 0);
    }
    
    /**
     * 带重试的请求执行
     */
    private String executeWithRetry(Object request, String json, int attempt) {
        try {
            String response = post("/v1/chat/completions", json);
            return parseContent(response);
        } catch (HermesAuthException e) {
            // 认证错误，不重试
            throw e;
        } catch (HermesNetworkException e) {
            if (attempt < config.getMaxRetries()) {
                int delay = (int) Math.pow(2, attempt) * 1000; // 指数退避: 1s, 2s, 4s
                log.warn("网络异常，第 {} 次重试，延迟 {}ms: {}", attempt + 1, delay, e.getMessage());
                sleep(delay);
                return executeWithRetry(request, json, attempt + 1);
            }
            throw e;
        } catch (HermesApiException e) {
            if (isRetryable(e) && attempt < config.getMaxRetries()) {
                int delay = (int) Math.pow(2, attempt) * 1000;
                log.warn("API 异常，第 {} 次重试，延迟 {}ms: {}", attempt + 1, delay, e.getMessage());
                sleep(delay);
                return executeWithRetry(request, json, attempt + 1);
            }
            throw e;
        }
    }
    
    private boolean isRetryable(HermesApiException e) {
        // 429 Rate Limit, 500/502/503 Server Error 可重试
        int status = e.getHttpStatus();
        return status == 429 || status == 500 || status == 502 || status == 503;
    }
    
    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 激活 Skill 并执行任务
     */
    public String activateSkill(String skillName, String task) {
        validateNotEmpty(skillName, "Skill 名称不能为空");
        validateNotEmpty(task, "任务描述不能为空");
        
        String systemPrompt = String.format(
            "先执行 skill_view(name='%s') 加载技能指南，然后严格按照技能指南执行任务。",
            skillName
        );
        return chatWithSystemPrompt(systemPrompt, task);
    }
    
    /**
     * 多轮对话会话
     */
    public ChatSession newSession() {
        return new ChatSession(this, config);
    }
    
    /**
     * 线程安全的多轮对话会话
     */
    public ThreadSafeChatSession newThreadSafeSession() {
        return new ThreadSafeChatSession(this, config);
    }
    
    /**
     * 健康检查
     */
    public boolean healthCheck() {
        try {
            String url = config.getBaseUrl() + "/api/hermes/health";
            Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (IOException e) {
            log.debug("健康检查失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 发送 HTTP POST 请求
     */
    private String post(String path, String json) {
        String url = config.getBaseUrl() + path;
        
        RequestBody body = RequestBody.create(json, MediaType.get("application/json"));
        Request request = new Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Authorization", "Bearer " + maskToken(config.getApiKey()))
            .addHeader("Content-Type", "application/json")
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            int status = response.code();
            
            if (status == 401) {
                throw new HermesAuthException("API Key 无效或已过期");
            }
            
            if (status == 403) {
                throw new HermesAuthException("API Key 权限不足");
            }
            
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "无响应体";
                throw new HermesApiException("API 请求失败", status);
            }
            
            return response.body().string();
        } catch (IOException e) {
            throw new HermesNetworkException("网络请求失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 解析响应内容
     */
    private String parseContent(String response) {
        try {
            JsonNode node = objectMapper.readTree(response);
            
            // 检查错误响应
            if (node.has("error")) {
                JsonNode error = node.get("error");
                String message = error.has("message") ? error.get("message").asText() : "未知错误";
                throw new HermesApiException("API_ERROR", message, -1);
            }
            
            JsonNode choices = node.get("choices");
            if (choices == null || !choices.isArray() || choices.isEmpty()) {
                throw new HermesApiException("响应格式错误：缺少 choices 字段", -1);
            }
            
            JsonNode message = choices.get(0).get("message");
            if (message == null || !message.has("content")) {
                throw new HermesApiException("响应格式错误：缺少 message.content 字段", -1);
            }
            
            return message.get("content").asText();
        } catch (HermesApiException e) {
            throw e;
        } catch (IOException e) {
            throw new HermesApiException("解析响应失败: " + e.getMessage(), -1, e);
        }
    }
    
    /**
     * 对象转 JSON
     */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new HermesApiException("序列化请求失败: " + e.getMessage(), -1, e);
        }
    }
    
    /**
     * 校验非空
     */
    private void validateNotEmpty(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
    
    /**
     * Token 脱敏
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "***";
        }
        return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
    }
    
    // ========== Getter ==========
    
    public HermesConfig getConfig() {
        return config;
    }
    
    public String getBaseUrl() {
        return config.getBaseUrl();
    }
    
    public String getModel() {
        return config.getModel();
    }
    
    /**
     * Builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
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
        
        public HermesClient build() {
            return new HermesClient(this);
        }
    }
}