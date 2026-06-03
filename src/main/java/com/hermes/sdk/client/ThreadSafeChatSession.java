package com.hermes.sdk.client;

import com.hermes.sdk.config.HermesConfig;
import com.hermes.sdk.logging.HermesLogger;
import com.hermes.sdk.logging.LogEvents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;

/**
 * 多轮对话会话
 * 
 * 线程安全版本，使用 CopyOnWriteArrayList + ReentrantLock 双重保障
 * 
 * 用法:
 *   ChatSession session = hermes.newThreadSafeSession();
 *   session.chat("我想写小说");
 *   session.chat("都市异能题材");
 *   String result = session.chat("开始写第1章");
 */
public class ThreadSafeChatSession {
    
    private static final Logger log = HermesLogger.get(ThreadSafeChatSession.class);
    
    private final HermesClient client;
    private final HermesConfig config;
    private final List<Message> history;
    private final ReadWriteLock lock;
    
    public ThreadSafeChatSession(HermesClient client, HermesConfig config) {
        this.client = client;
        this.config = config;
        this.history = new ArrayList<>();
        this.lock = new ReentrantReadWriteLock();
    }
    
    /**
     * 发送消息
     */
    public String chat(String message) {
        log.debug("[{}] 发送消息: {}", LogEvents.CHAT_REQUEST, maskContent(message));
        
        lock.writeLock().lock();
        try {
            history.add(new Message("user", message));
        } finally {
            lock.writeLock().unlock();
        }
        
        String response = sendToHermes();
        
        lock.writeLock().lock();
        try {
            history.add(new Message("assistant", response));
        } finally {
            lock.writeLock().unlock();
        }
        
        log.debug("[{}] 收到响应: {} chars, historySize={}", 
            LogEvents.CHAT_RESPONSE, response.length(), history.size());
        return response;
    }
    
    /**
     * 带 System Prompt 的多轮对话
     */
    public String chatWithSystem(String systemPrompt, String message) {
        log.debug("[{}] systemPrompt={}, 消息: {}", 
            LogEvents.CHAT_REQUEST, systemPrompt != null ? "有" : "无", maskContent(message));
        history.add(new Message("user", message));
        
        String response = client.chatWithSystemPrompt(systemPrompt, buildContext());
        history.add(new Message("assistant", response));
        
        log.debug("[{}] 收到响应: {} chars", LogEvents.CHAT_RESPONSE, response.length());
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
        lock.readLock().lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(history));
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 清空对话历史
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            history.clear();
        } finally {
            lock.writeLock().unlock();
        }
        log.info("[{}] history cleared, historySize=0", LogEvents.SESSION_CLEAR);
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
    
    private String maskContent(String content) {
        if (content == null) return "null";
        if (content.length() <= 50) return content;
        return content.substring(0, 50) + "...(length=" + content.length() + ")";
    }
}