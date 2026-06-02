package com.hermes.sdk.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hermes.sdk.config.HermesConfig;
import com.hermes.sdk.dto.Run;
import com.hermes.sdk.exception.HermesApiException;
import com.hermes.sdk.exception.HermesNetworkException;
import com.hermes.sdk.logging.HermesLogger;
import com.hermes.sdk.logging.LogEvents;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;

/**
 * Run 服务
 * 
 * 对应 Hermes API:
 * - POST /v1/runs                  — 启动异步 Agent，立即返回 run_id
 * - GET  /v1/runs/{run_id}         — 查询运行状态
 * - GET  /v1/runs/{run_id}/events  — SSE 生命周期事件流
 * - POST /v1/runs/{run_id}/approval — 解决待审批
 * - POST /v1/runs/{run_id}/stop    — 中断运行中的 Agent
 */
public class RunService {
    
    private static final Logger log = HermesLogger.get(RunService.class);
    
    private final HermesConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper mapper;
    
    public RunService(HermesConfig config, OkHttpClient httpClient, ObjectMapper mapper) {
        this.config = config;
        this.httpClient = httpClient;
        this.mapper = mapper;
    }
    
    /**
     * 启动异步 Agent（立即返回 run_id）
     * POST /v1/runs
     */
    public Map<String, Object> startRun(String message, String model, String mode, 
                                         String sessionId, String systemPrompt, Boolean stream) {
        log.info("[{}] 启动异步 Agent: message={}, model={}, mode={}", 
            LogEvents.RUN_START, 
            maskContent(message), model, mode);
        
        HttpUrl url = HttpUrl.parse(config.getBaseUrl())
            .newBuilder()
            .addPathSegment("v1")
            .addPathSegment("runs")
            .build();
        
        // Build request body
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("message", message);
        if (model != null) body.put("model", model);
        if (mode != null) body.put("mode", mode);
        if (sessionId != null) body.put("session_id", sessionId);
        if (systemPrompt != null) body.put("system_prompt", systemPrompt);
        if (stream != null) body.put("stream", stream);

        String jsonBody;
        try {
            jsonBody = mapper.writeValueAsString(body);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new HermesApiException("RUN_START: JSON encode error", -1);
        }

        RequestBody requestBody = RequestBody.create(jsonBody, MediaType.parse("application/json"));
        
        Request request = new Request.Builder()
            .url(url)
            .post(requestBody)
            .build();
        
        try (Response resp = httpClient.newCall(request).execute()) {
            String respBody = resp.body().string();
            
            if (!resp.isSuccessful()) {
                log.error("[{}] 失败: http={}, body={}", LogEvents.RUN_START, resp.code(), respBody);
                throw new HermesApiException("RUN_START: " + respBody, resp.code());
            }
            
            Map<String, Object> result = mapper.readValue(respBody, 
                new TypeReference<Map<String, Object>>() {});
            String runId = (String) result.get("run_id");
            log.info("[{}] 成功: run_id={}", LogEvents.RUN_START, runId);
            return result;
            
        } catch (HermesApiException e) {
            throw e;
        } catch (IOException e) {
            log.error("[{}] IO 异常: {}", LogEvents.RUN_START, e.getMessage());
            throw new HermesNetworkException("RUN_START", e);
        }
    }
    
    /**
     * 启动异步 Agent（简单版）
     */
    public Map<String, Object> startRun(String message) {
        return startRun(message, null, null, null, null, null);
    }
    
    /**
     * 查询运行状态
     * GET /v1/runs/{run_id}
     */
    public Map<String, Object> getRunStatus(String runId) {
        log.info("[{}] 查询运行状态: run_id={}", LogEvents.RUN_STATUS, runId);
        
        HttpUrl url = HttpUrl.parse(config.getBaseUrl())
            .newBuilder()
            .addPathSegment("v1")
            .addPathSegment("runs")
            .addPathSegment(runId)
            .build();
        
        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();
        
        try (Response resp = httpClient.newCall(request).execute()) {
            String body = resp.body().string();
            
            if (!resp.isSuccessful()) {
                log.error("[{}] 失败: http={}, body={}", LogEvents.RUN_STATUS, resp.code(), body);
                throw new HermesApiException("RUN_STATUS: " + body, resp.code());
            }
            
            Map<String, Object> status = mapper.readValue(body, 
                new TypeReference<Map<String, Object>>() {});
            log.info("[{}] 成功: status={}", LogEvents.RUN_STATUS, status.get("status"));
            return status;
            
        } catch (HermesApiException e) {
            throw e;
        } catch (IOException e) {
            log.error("[{}] IO 异常: {}", LogEvents.RUN_STATUS, e.getMessage());
            throw new HermesNetworkException("RUN_STATUS", e);
        }
    }
    
    /**
     * 解决待审批
     * POST /v1/runs/{run_id}/approval
     */
    public Map<String, Object> approveRun(String runId, String approval) {
        log.info("[{}] 解决待审批: run_id={}, approval={}", LogEvents.RUN_APPROVAL, runId, approval);
        
        HttpUrl url = HttpUrl.parse(config.getBaseUrl())
            .newBuilder()
            .addPathSegment("v1")
            .addPathSegment("runs")
            .addPathSegment(runId)
            .addPathSegment("approval")
            .build();
        
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("approval", approval);

        String jsonBody;
        try {
            jsonBody = mapper.writeValueAsString(body);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new HermesApiException("RUN_APPROVAL: JSON encode error", -1);
        }

        RequestBody requestBody = RequestBody.create(jsonBody, MediaType.parse("application/json"));
        
        Request request = new Request.Builder()
            .url(url)
            .post(requestBody)
            .build();
        
        try (Response resp = httpClient.newCall(request).execute()) {
            String respBody = resp.body().string();
            
            if (!resp.isSuccessful()) {
                log.error("[{}] 失败: http={}, body={}", LogEvents.RUN_APPROVAL, resp.code(), respBody);
                throw new HermesApiException("RUN_APPROVAL: " + respBody, resp.code());
            }
            
            Map<String, Object> result = mapper.readValue(respBody, 
                new TypeReference<Map<String, Object>>() {});
            log.info("[{}] 成功", LogEvents.RUN_APPROVAL);
            return result;
            
        } catch (HermesApiException e) {
            throw e;
        } catch (IOException e) {
            log.error("[{}] IO 异常: {}", LogEvents.RUN_APPROVAL, e.getMessage());
            throw new HermesNetworkException("RUN_APPROVAL", e);
        }
    }
    
    /**
     * 中断运行中的 Agent
     * POST /v1/runs/{run_id}/stop
     */
    public Map<String, Object> stopRun(String runId) {
        log.info("[{}] 中断 Agent: run_id={}", LogEvents.RUN_STOP, runId);
        
        HttpUrl url = HttpUrl.parse(config.getBaseUrl())
            .newBuilder()
            .addPathSegment("v1")
            .addPathSegment("runs")
            .addPathSegment(runId)
            .addPathSegment("stop")
            .build();
        
        Request request = new Request.Builder()
            .url(url)
            .post(RequestBody.create("", MediaType.parse("application/json")))
            .build();
        
        try (Response resp = httpClient.newCall(request).execute()) {
            String body = resp.body().string();
            
            if (!resp.isSuccessful()) {
                log.error("[{}] 失败: http={}, body={}", LogEvents.RUN_STOP, resp.code(), body);
                throw new HermesApiException("RUN_STOP: " + body, resp.code());
            }
            
            Map<String, Object> result = mapper.readValue(body, 
                new TypeReference<Map<String, Object>>() {});
            log.info("[{}] 成功", LogEvents.RUN_STOP);
            return result;
            
        } catch (HermesApiException e) {
            throw e;
        } catch (IOException e) {
            log.error("[{}] IO 异常: {}", LogEvents.RUN_STOP, e.getMessage());
            throw new HermesNetworkException("RUN_STOP", e);
        }
    }
    
    /**
     * 脱敏内容
     */
    private String maskContent(String content) {
        if (content == null) return "null";
        return content.length() > 50 
            ? content.substring(0, 50) + "...(length=" + content.length() + ")" 
            : content;
    }
}