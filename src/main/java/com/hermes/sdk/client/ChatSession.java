package com.hermes.sdk.client;

import com.hermes.sdk.config.HermesConfig;
import com.hermes.sdk.exception.HermesException;
import com.hermes.sdk.logging.HermesLogger;
import com.hermes.sdk.logging.LogEvents;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;

/**
 * 多轮对话会话
 * 
 * 非线程安全，适用于单线程场景
 */
public class ChatSession {
    
    private static final Logger log = HermesLogger.get(ChatSession.class);
    
    private final HermesClient client;
    private final HermesConfig config;
    private final List<String> history;
    
    public ChatSession(HermesClient client, HermesConfig config) {
        this.client = client;
        this.config = config;
        this.history = new ArrayList<>();
    }
    
    /**
     * 发送消息并记录历史
     */
    public String chat(String message) {
        log.debug("[{}] 发送消息: {}", LogEvents.CHAT_REQUEST, maskContent(message));
        try {
            String response = client.chatWithSystemPrompt(
                buildContextPrompt(),
                message
            );
            history.add("user:" + message);
            history.add("assistant:" + response);
            log.debug("[{}] 收到响应: {} chars, historySize={}", 
                LogEvents.CHAT_RESPONSE, response.length(), history.size());
            return response;
        } catch (HermesException e) {
            log.error("[{}] 失败: {}", LogEvents.CHAT_ERROR, e.getMessage());
            throw e;
        }
    }
    
    /**
     * 构建上下文 Prompt
     */
    private String buildContextPrompt() {
        if (history.isEmpty()) {
            return null;
        }
        return "对话历史：\n" + String.join("\n", history);
    }
    
    /**
     * 清空历史
     */
    public void clear() {
        history.clear();
        log.info("[{}] history cleared", LogEvents.SESSION_CLEAR);
    }
    
    /**
     * 获取历史消息数
     */
    public int size() {
        return history.size() / 2; // user + assistant
    }
    
    /**
     * 获取不可修改的历史副本
     */
    public List<String> getHistory() {
        return Collections.unmodifiableList(history);
    }
    
    private String maskContent(String content) {
        if (content == null) return "null";
        if (content.length() <= 50) return content;
        return content.substring(0, 50) + "...(length=" + content.length() + ")";
    }
}