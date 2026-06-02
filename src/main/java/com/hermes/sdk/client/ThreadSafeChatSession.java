package com.hermes.sdk.client;

import com.hermes.sdk.config.HermesConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 多轮对话会话
 * 
 * 线程安全版本，使用 CopyOnWriteArrayList
 * 
 * 用法:
 *   ChatSession session = hermes.newThreadSafeSession();
 *   session.chat("我想写小说");
 *   session.chat("都市异能题材");
 *   String result = session.chat("开始写第1章");
 */
@Slf4j
public class ThreadSafeChatSession {
    
    private final HermesClient client;
    private final HermesConfig config;
    private final CopyOnWriteArrayList<Message> history;
    
    public ThreadSafeChatSession(HermesClient client, HermesConfig config) {
        this.client = client;
        this.config = config;
        this.history = new CopyOnWriteArrayList<>();
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
     * 获取对话历史（只读副本）
     */
    public List<Message> getHistory() {
        return Collections.unmodifiableList(new ArrayList<>(history));
    }
    
    /**
     * 清空对话历史
     */
    public void clear() {
        history.clear();
    }
    
    /**
     * 消息数量
     */
    public int size() {
        return history.size();
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