package com.hermes.sdk.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Session Message
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {
    private String id;
    private String sessionId;
    private String role;        // user / assistant / system / tool
    private String content;
    private String toolCallId;
    private String toolCalls;    // JSON string
    private String toolName;
    private String timestamp;
    private String tokenCount;
    private String finishReason;
}