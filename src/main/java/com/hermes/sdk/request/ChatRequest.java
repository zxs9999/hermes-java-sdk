package com.hermes.sdk.request;

import lombok.Data;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * Chat 请求体（OpenAI 兼容）
 */
@Data
public class ChatRequest {
    
    private String model;
    private List<Map<String, String>> messages;
    private double temperature;
    private int maxTokens;
    
    public ChatRequest() {
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