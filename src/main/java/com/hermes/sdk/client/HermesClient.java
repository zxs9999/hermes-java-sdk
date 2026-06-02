package com.hermes.sdk.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.hermes.sdk.config.HermesConfig;
import com.hermes.sdk.request.ChatRequest;
import com.hermes.sdk.response.ChatResponse;
import okhttp3.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;

/**
 * Hermes Java SDK 客户端
 * 
 * 用法:
 *   HermesClient hermes = new HermesClient("http://localhost:8080", "api-key");
 *   String result = hermes.chat("分析代码");
 * 
 * 激活 Skill:
 *   String result = hermes.activateSkill("skill-name", "task description");
 */
@Slf4j
public class HermesClient {
    
    private final HermesConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public HermesClient(String baseUrl, String apiKey) {
        this.config = new HermesConfig(baseUrl, apiKey);
        this.httpClient = buildHttpClient();
        this.objectMapper = new ObjectMapper();
    }
    
    public HermesClient(HermesConfig config) {
        this.config = config;
        this.httpClient = buildHttpClient();
        this.objectMapper = new ObjectMapper();
    }
    
    private OkHttpClient buildHttpClient() {
        return new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(config.getConnectTimeout()))
            .readTimeout(Duration.ofSeconds(config.getReadTimeout()))
            .build();
    }
    
    /**
     * 简单聊天
     */
    public String chat(String message) {
        return chatWithSystemPrompt(null, message);
    }
    
    /**
     * 带 System Prompt 的聊天
     */
    public String chatWithSystemPrompt(String systemPrompt, String userMessage) {
        ChatRequest request = new ChatRequest();
        request.setModel(config.getModel());
        request.setTemperature(config.getTemperature());
        request.setMaxTokens(config.getMaxTokens());
        
        if (systemPrompt != null) {
            request.addSystemMessage(systemPrompt);
        }
        request.addUserMessage(userMessage);
        
        String json = toJson(request);
        String response = post("/v1/chat/completions", json);
        
        return parseContent(response);
    }
    
    /**
     * 激活 Skill 并执行任务
     * 
     * @param skillName Skill 名称
     * @param task 任务描述
     * @return 执行结果
     */
    public String activateSkill(String skillName, String task) {
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
     * 发送 HTTP POST 请求
     */
    private String post(String path, String json) {
        String url = config.getBaseUrl() + path;
        
        RequestBody body = RequestBody.create(json, MediaType.get("application/json"));
        Request request = new Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Authorization", "Bearer " + config.getApiKey())
            .addHeader("Content-Type", "application/json")
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Hermes 请求失败: " + response.code() + " " + response.message());
            }
            return response.body().string();
        } catch (IOException e) {
            throw new RuntimeException("Hermes 请求异常: " + e.getMessage(), e);
        }
    }
    
    /**
     * 解析响应内容
     */
    private String parseContent(String response) {
        try {
            JsonNode node = objectMapper.readTree(response);
            JsonNode choices = node.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                return choices.get(0).get("message").get("content").asText();
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeException("解析响应失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 对象转 JSON
     */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("序列化请求失败: " + e.getMessage(), e);
        }
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
}