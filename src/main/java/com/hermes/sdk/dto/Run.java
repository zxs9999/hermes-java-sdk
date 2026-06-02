package com.hermes.sdk.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Hermes Run
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Run {
    
    private String runId;
    private String status;       // queued, running, completed, failed, stopped
    private String sessionId;
    private String model;
    private String mode;
    private Long createdAt;
    private Long updatedAt;
    private Long completedAt;
    private String error;
}

/**
 * Run 事件（来自 SSE）
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class RunEvent {
    private String event;         // run.created, run.completed, tool.call, etc.
    private String runId;
    private String sessionId;
    private Map<String, Object> data;
}

/**
 * Run 启动请求
 */
@Data
class RunRequest {
    private String message;
    private String model;
    private String mode;
    private String sessionId;
    private String sessionKey;
    private String systemPrompt;
    private Boolean stream;
    private List<Map<String, Object>> tools;
}

/**
 * Run 状态响应
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class RunStatus {
    private String runId;
    private String status;
    private String sessionId;
    private Long createdAt;
    private Long updatedAt;
    private String error;
}