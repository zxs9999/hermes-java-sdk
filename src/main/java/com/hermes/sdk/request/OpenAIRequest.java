package com.hermes.sdk.request;

import lombok.Data;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * OpenAI 兼容请求体
 */
@Data
public class OpenAIRequest {
    
    private String model;
    private List<Map<String, String>> messages;
    private double temperature;
    private int maxTokens;
    
    public OpenAIRequest() {
        this.messages = new ArrayList<>();
        this.temperature = 0.7;
        this.maxTokens = 4096;
    }
    
    public void addMessage(String role, String content) {
        this.messages.add(Map.of("role", role, "content", content));
    }
    
    public void addSystemMessage(String content) {
        addMessage("system", content);
    }
    
    public void addUserMessage(String content) {
        addMessage("user", content);
    }
}