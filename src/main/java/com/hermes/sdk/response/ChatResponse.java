package com.hermes.sdk.response;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Chat 响应体（OpenAI 兼容）
 */
@Data
public class ChatResponse {
    
    private String id;
    private String object;
    private long created;
    private String model;
    private List<Choice> choices;
    private Map<String, Object> usage;
    
    @Data
    public static class Choice {
        private int index;
        private Map<String, String> message;
        private String finishReason;
    }
    
    /**
     * 获取消息内容
     */
    public String getContent() {
        if (choices != null && !choices.isEmpty()) {
            return choices.get(0).getMessage().get("content");
        }
        return null;
    }
}