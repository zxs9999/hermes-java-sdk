package com.hermes.sdk.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hermes.sdk.config.HermesConfig;
import com.hermes.sdk.dto.*;
import com.hermes.sdk.exception.HermesApiException;
import com.hermes.sdk.logging.HermesLogger;
import com.hermes.sdk.logging.LogEvents;
import okhttp3.OkHttpClient;

import java.util.List;
import java.util.Map;
import com.hermes.sdk.exception.HermesException;
import org.slf4j.Logger;

/**
 * Hermes 原始 API 服务
 * 
 * 一对一对应 gateway/platforms/api_server.py 的 HTTP 接口
 * 直接调用，不做业务封装
 */
public class HermesApi extends ApiServerCore {
    
    private static final Logger log = HermesLogger.get(HermesApi.class);
    
    public HermesApi(HermesConfig config, OkHttpClient httpClient, ObjectMapper mapper) {
        super(config, httpClient, mapper);
    }
    
    // ==================== Skills & Tools ====================
    
    /** GET /v1/skills */
    public List<Skill> listSkills() throws HermesException {
        log.info("[{}] 列出所有 Skills", LogEvents.SKILLS_LIST);
        String body = get("v1/skills");
        try {
            JsonNode node = mapper.readTree(body);
            List<Skill> skills = mapper.convertValue(node.get("data"), new TypeReference<List<Skill>>() {});
            log.info("[{}] 成功: count={}", LogEvents.SKILLS_LIST, skills.size());
            return skills;
        } catch (Exception e) {
            throw new HermesApiException("PARSE_SKILLS: " + e.getMessage(), -1);
        }
    }
    
    /** GET /v1/toolsets */
    public List<Toolset> listToolsets() throws HermesException {
        log.info("[{}] 列出所有 Toolsets", LogEvents.TOOLSETS_LIST);
        String body = get("v1/toolsets");
        try {
            JsonNode node = mapper.readTree(body);
            List<Toolset> toolsets = mapper.convertValue(node.get("data"), new TypeReference<List<Toolset>>() {});
            log.info("[{}] 成功: count={}", LogEvents.TOOLSETS_LIST, toolsets.size());
            return toolsets;
        } catch (Exception e) {
            throw new HermesApiException("PARSE_TOOLSETS: " + e.getMessage(), -1);
        }
    }
    
    /** GET /v1/capabilities */
    public Map<String, Object> getCapabilities() throws HermesException {
        log.info("[{}] 获取 API 能力清单", LogEvents.CAPABILITIES_GET);
        String body = get("v1/capabilities");
        try {
            return mapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new HermesApiException("PARSE_CAPABILITIES: " + e.getMessage(), -1);
        }
    }
    
    /** GET /v1/models */
    public Map<String, Object> listModels() throws HermesException {
        log.info("[{}] 获取可用模型列表", LogEvents.MODELS_LIST);
        String body = get("v1/models");
        try {
            return mapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new HermesApiException("PARSE_MODELS: " + e.getMessage(), -1);
        }
    }
    
    // ==================== Health ====================
    
    /** GET /health */
    public boolean health() throws HermesException {
        log.info("[{}] 健康检查", LogEvents.HEALTH_CHECK);
        try {
            get("health");
            return true;
        } catch (HermesException e) {
            log.warn("[{}] 健康检查失败: {}", LogEvents.HEALTH_CHECK, e.getMessage());
            return false;
        }
    }
    
    /** GET /health/detailed */
    public Map<String, Object> healthDetailed() throws HermesException {
        log.info("[{}] 详细健康检查", LogEvents.HEALTH_CHECK_DETAILED);
        String body = get("health/detailed");
        try {
            return mapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new HermesApiException("PARSE_HEALTH: " + e.getMessage(), -1);
        }
    }
    
    // ==================== Sessions ====================
    
    /** GET /api/sessions?limit=&offset= */
    public List<Session> listSessions(int limit, int offset) throws HermesException {
        log.info("[{}] 列出 Sessions: limit={}, offset={}", LogEvents.SESSION_LIST, limit, offset);
        String body = get("api/sessions", Map.of("limit", String.valueOf(limit), "offset", String.valueOf(offset)));
        try {
            JsonNode node = mapper.readTree(body);
            List<Session> sessions = mapper.convertValue(node.get("data"), new TypeReference<List<Session>>() {});
            log.info("[{}] 成功: count={}", LogEvents.SESSION_LIST, sessions.size());
            return sessions;
        } catch (Exception e) {
            throw new HermesApiException("PARSE_SESSIONS: " + e.getMessage(), -1);
        }
    }
    
    /** GET /api/sessions/{sessionId} */
    public Session getSession(String sessionId) throws HermesException {
        log.info("[{}] 获取 Session: {}", LogEvents.SESSION_GET, sessionId);
        String body = get("api/sessions/" + sessionId);
        try {
            JsonNode node = mapper.readTree(body);
            return mapper.treeToValue(node.get("session"), Session.class);
        } catch (Exception e) {
            throw new HermesApiException("PARSE_SESSION: " + e.getMessage(), -1);
        }
    }
    
    /** POST /api/sessions */
    public Session createSession(String sessionId, String model, String title, String systemPrompt) throws HermesException {
        log.info("[{}] 创建 Session: id={}", LogEvents.SESSION_CREATE, sessionId);
        Map<String, Object> req = new java.util.HashMap<>();
        if (sessionId != null) req.put("session_id", sessionId);
        if (model != null) req.put("model", model);
        if (title != null) req.put("title", title);
        if (systemPrompt != null) req.put("system_prompt", systemPrompt);
        
        String body = post("api/sessions", req);
        try {
            JsonNode node = mapper.readTree(body);
            return mapper.treeToValue(node.get("session"), Session.class);
        } catch (Exception e) {
            throw new HermesApiException("PARSE_SESSION: " + e.getMessage(), -1);
        }
    }
    
    /** DELETE /api/sessions/{sessionId} */
    public void deleteSession(String sessionId) throws HermesException {
        log.info("[{}] 删除 Session: {}", LogEvents.SESSION_DELETE, sessionId);
        delete("api/sessions/" + sessionId);
        log.info("[{}] 删除成功: {}", LogEvents.SESSION_DELETE, sessionId);
    }
    
    /** GET /api/sessions/{sessionId}/messages */
    public List<Message> getSessionMessages(String sessionId) throws HermesException {
        log.info("[{}] 获取 Session Messages: {}", LogEvents.SESSION_MESSAGES, sessionId);
        String body = get("api/sessions/" + sessionId + "/messages");
        try {
            JsonNode node = mapper.readTree(body);
            return mapper.convertValue(node.get("data"), new TypeReference<List<Message>>() {});
        } catch (Exception e) {
            throw new HermesApiException("PARSE_MESSAGES: " + e.getMessage(), -1);
        }
    }
    
    // ==================== Runs ====================
    
    /** POST /v1/runs */
    public Map<String, Object> startRun(String message, String model, String mode, 
                                        String sessionId, String systemPrompt, Boolean stream) throws HermesException {
        log.info("[{}] 启动 Run: message={}", LogEvents.RUN_START, mask(message));
        Map<String, Object> req = new java.util.HashMap<>();
        req.put("message", message);
        if (model != null) req.put("model", model);
        if (mode != null) req.put("mode", mode);
        if (sessionId != null) req.put("session_id", sessionId);
        if (systemPrompt != null) req.put("system_prompt", systemPrompt);
        if (stream != null) req.put("stream", stream);
        
        String body = post("v1/runs", req);
        try {
            return mapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new HermesApiException("PARSE_RUN: " + e.getMessage(), -1);
        }
    }
    
    /** GET /v1/runs/{runId} */
    public Map<String, Object> getRunStatus(String runId) throws HermesException {
        log.info("[{}] 查询 Run 状态: {}", LogEvents.RUN_STATUS, runId);
        String body = get("v1/runs/" + runId);
        try {
            return mapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new HermesApiException("PARSE_RUN_STATUS: " + e.getMessage(), -1);
        }
    }
    
    /** POST /v1/runs/{runId}/approval */
    public Map<String, Object> approveRun(String runId, String approval) throws HermesException {
        log.info("[{}] 审批 Run: {}", LogEvents.RUN_APPROVAL, runId);
        String body = post("v1/runs/" + runId + "/approval", Map.of("approval", approval));
        try {
            return mapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new HermesApiException("PARSE_RUN_APPROVAL: " + e.getMessage(), -1);
        }
    }
    
    /** POST /v1/runs/{runId}/stop */
    public Map<String, Object> stopRun(String runId) throws HermesException {
        log.info("[{}] 停止 Run: {}", LogEvents.RUN_STOP, runId);
        String body = post("v1/runs/" + runId + "/stop", Map.of());
        try {
            return mapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new HermesApiException("PARSE_RUN_STOP: " + e.getMessage(), -1);
        }
    }
    
    // ==================== Helpers ====================
    
    private String mask(String s) {
        if (s == null) return "null";
        return s.length() > 50 ? s.substring(0, 50) + "..." : s;
    }
}