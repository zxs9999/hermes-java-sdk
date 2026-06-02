package com.hermes.sdk.client;

import com.hermes.sdk.config.HermesConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 多轮对话会话
 * 
 * 用法:
 *   ChatSession session = hermes.newSession();
 *   session.chat("我想写小说");
 *   session.chat("都市异能题材");
 *   String result = session.chat("开始写第1章");
 */
@Slf4j
public class ChatSession {
    
    private final HermesClient client;
    private final HermesConfig config;
    private final List<Message> history;
    
    public ChatSession(HermesClient client, HermesConfig config) {
        this.client = client;
        this.config = config;
        this.history = new ArrayList<>();
    }
    
    /**
     * 发送消息
     */
    public String chat(String message) {
        history.add(new Message("user", message));
        
        String response = sendToHermes();
        history.add(new Message("assistant", response));
        
        return response;
    }
    
    /**
     * 带 System Prompt 的多轮对话
     */
    public String chatWithSystem(String systemPrompt, String message) {
        history.add(new Message("user", message));
        
        String response = client.chatWithSystemPrompt(systemPrompt, buildContext());
        history.add(new Message("assistant", response));
        
        return response;
    }
    
    /**
     * 发送完整对话历史
     */
    private String sendToHermes() {
        String context = buildContext();
        return client.chatWithSystemPrompt(null, context);
    }
    
    /**
     * 构建上下文（保留对话历史）
     */
    private String buildContext() {
        StringBuilder sb = new StringBuilder();
        for (Message msg : history) {
            String role = msg.role.equals("user") ? "用户" : "助手";
            sb.append(role).append("：").append(msg.content).append("\n\n");
        }
        return sb.toString();
    }
    
    /**
     * 获取对话历史
     */
    public List<Message> getHistory() {
        return new ArrayList<>(history);
    }
    
    /**
     * 清空对话历史
     */
    public void clear() {
        history.clear();
    }
    
    /**
     * 消息对象
     */
    public static class Message {
        public final String role;
        public final String content;
        
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}