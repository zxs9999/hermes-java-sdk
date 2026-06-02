package com.hermes.sdk.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Session Message
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {
    private String id;
    private String sessionId;
    private String role;
    private String content;
    private String toolCallId;
    private String toolCalls;
    private String toolName;
    private String timestamp;
    private String tokenCount;
    private String finishReason;
    
    public Message() {}
    
    public String getId() { return id; }
    public void setId(String v) { this.id = v; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String v) { this.sessionId = v; }
    public String getRole() { return role; }
    public void setRole(String v) { this.role = v; }
    public String getContent() { return content; }
    public void setContent(String v) { this.content = v; }
    public String getToolCallId() { return toolCallId; }
    public void setToolCallId(String v) { this.toolCallId = v; }
    public String getToolCalls() { return toolCalls; }
    public void setToolCalls(String v) { this.toolCalls = v; }
    public String getToolName() { return toolName; }
    public void setToolName(String v) { this.toolName = v; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String v) { this.timestamp = v; }
    public String getTokenCount() { return tokenCount; }
    public void setTokenCount(String v) { this.tokenCount = v; }
    public String getFinishReason() { return finishReason; }
    public void setFinishReason(String v) { this.finishReason = v; }
}
