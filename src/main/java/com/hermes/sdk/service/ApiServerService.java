package com.hermes.sdk.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hermes.sdk.config.HermesConfig;
import com.hermes.sdk.dto.Skill;
import com.hermes.sdk.dto.Toolset;
import com.hermes.sdk.exception.HermesApiException;
import com.hermes.sdk.exception.HermesException;
import com.hermes.sdk.logging.HermesLogger;
import com.hermes.sdk.logging.LogEvents;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * API Server 服务
 * 
 * 对应 gateway/platforms/api_server.py 的 HTTP 接口：
 * - GET  /v1/skills         — 列出所有 Skills
 * - GET  /v1/toolsets       — 列出所有 Toolsets
 * - GET  /v1/capabilities   — API 能力清单
 * - GET  /v1/models         — 可用模型列表
 * - GET  /health           — 健康检查
 * - GET  /health/detailed   — 详细健康状态
 * 
 * 注意：Sessions/Runs 接口见 SessionService / RunService
 */
public class ApiServerService {
    
    private static final Logger log = HermesLogger.get(ApiServerService.class);
    
    private final HermesConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper mapper;
    
    public ApiServerService(HermesConfig config, OkHttpClient httpClient, ObjectMapper mapper) {
        this.config = config;
        this.httpClient = httpClient;
        this.mapper = mapper;
    }
    
    // ==================== Skills ====================
    
    /**
     * 列出所有 Skills
     * GET /v1/skills
     */
    public List<Skill> listSkills() {
        log.info("[{}] 列出所有 Skills", LogEvents.SKILLS_LIST);
        
        HttpUrl url = HttpUrl.parse(config.getBaseUrl())
            .newBuilder()
            .addPathSegment("v1")
            .addPathSegment("skills")
            .build();
        
        Request request = new Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer " + config.getApiKey())
            .get()
            .build();
        
        try (Response resp = httpClient.newCall(request).execute()) {
            String body = resp.body().string();
            
            if (!resp.isSuccessful()) {
                log.error("[{}] 失败: http={}, body={}", LogEvents.SKILLS_LIST, resp.code(), body);
                throw new HermesApiException("SKILLS_LIST", resp.code(), body);
            }
            
            JsonNode node = mapper.readTree(body);
            List<Skill> skills = mapper.convertValue(
                node.get("data"),
                new TypeReference<List<Skill>>() {}
            );
            log.info("[{}] 成功: count={}", LogEvents.SKILLS_LIST, skills.size());
            return skills;
            
        } catch (HermesException e) {
            throw e;
        } catch (IOException e) {
            log.error("[{}] IO 异常: {}", LogEvents.SKILLS_LIST, e.getMessage());
            throw new HermesNetworkException("SKILLS_LIST", e);
        }
    }
    
    // ==================== Toolsets ====================
    
    /**
     * 列出所有 Toolsets
     * GET /v1/toolsets
     */
    public List<Toolset> listToolsets() {
        log.info("[{}] 列出所有 Toolsets", LogEvents.TOOLSETS_LIST);
        
        HttpUrl url = HttpUrl.parse(config.getBaseUrl())
            .newBuilder()
            .addPathSegment("v1")
            .addPathSegment("toolsets")
            .build();
        
        Request request = new Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer " + config.getApiKey())
            .get()
            .build();
        
        try (Response resp = httpClient.newCall(request).execute()) {
            String body = resp.body().string();
            
            if (!resp.isSuccessful()) {
                log.error("[{}] 失败: http={}, body={}", LogEvents.TOOLSETS_LIST, resp.code(), body);
                throw new HermesApiException("TOOLSETS_LIST", resp.code(), body);
            }
            
            JsonNode node = mapper.readTree(body);
            List<Toolset> toolsets = mapper.convertValue(
                node.get("data"),
                new TypeReference<List<Toolset>>() {}
            );
            log.info("[{}] 成功: count={}", LogEvents.TOOLSETS_LIST, toolsets.size());
            return toolsets;
            
        } catch (HermesException e) {
            throw e;
        } catch (IOException e) {
            log.error("[{}] IO 异常: {}", LogEvents.TOOLSETS_LIST, e.getMessage());
            throw new HermesNetworkException("TOOLSETS_LIST", e);
        }
    }
    
    // ==================== Capabilities ====================
    
    /**
     * 获取 API 能力清单
     * GET /v1/capabilities
     */
    public Map<String, Object> getCapabilities() {
        log.info("[{}] 获取 API 能力清单", LogEvents.CAPABILITIES_GET);
        
        HttpUrl url = HttpUrl.parse(config.getBaseUrl())
            .newBuilder()
            .addPathSegment("v1")
            .addPathSegment("capabilities")
            .build();
        
        Request request = new Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer " + config.getApiKey())
            .get()
            .build();
        
        try (Response resp = httpClient.newCall(request).execute()) {
            String body = resp.body().string();
            
            if (!resp.isSuccessful()) {
                log.error("[{}] 失败: http={}, body={}", LogEvents.CAPABILITIES_GET, resp.code(), body);
                throw new HermesApiException("CAPABILITIES_GET", resp.code(), body);
            }
            
            Map<String, Object> capabilities = mapper.readValue(body, 
                new TypeReference<Map<String, Object>>() {});
            log.info("[{}] 成功", LogEvents.CAPABILITIES_GET);
            return capabilities;
            
        } catch (HermesException e) {
            throw e;
        } catch (IOException e) {
            log.error("[{}] IO 异常: {}", LogEvents.CAPABILITIES_GET, e.getMessage());
            throw new HermesNetworkException("CAPABILITIES_GET", e);
        }
    }
    
    // ==================== Models ====================
    
    /**
     * 获取可用模型列表
     * GET /v1/models
     */
    public Map<String, Object> listModels() {
        log.info("[{}] 获取可用模型列表", LogEvents.MODELS_LIST);
        
        HttpUrl url = HttpUrl.parse(config.getBaseUrl())
            .newBuilder()
            .addPathSegment("v1")
            .addPathSegment("models")
            .build();
        
        Request request = new Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer " + config.getApiKey())
            .get()
            .build();
        
        try (Response resp = httpClient.newCall(request).execute()) {
            String body = resp.body().string();
            
            if (!resp.isSuccessful()) {
                log.error("[{}] 失败: http={}, body={}", LogEvents.MODELS_LIST, resp.code(), body);
                throw new HermesApiException("MODELS_LIST", resp.code(), body);
            }
            
            Map<String, Object> models = mapper.readValue(body, 
                new TypeReference<Map<String, Object>>() {});
            log.info("[{}] 成功", LogEvents.MODELS_LIST);
            return models;
            
        } catch (HermesException e) {
            throw e;
        } catch (IOException e) {
            log.error("[{}] IO 异常: {}", LogEvents.MODELS_LIST, e.getMessage());
            throw new HermesNetworkException("MODELS_LIST", e);
        }
    }
    
    // ==================== Health ====================
    
    /**
     * 健康检查
     * GET /health
     */
    public boolean health() {
        log.info("[{}] 健康检查", LogEvents.HEALTH_CHECK);
        
        HttpUrl url = HttpUrl.parse(config.getBaseUrl())
            .newBuilder()
            .addPathSegment("health")
            .build();
        
        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();
        
        try (Response resp = httpClient.newCall(request).execute()) {
            boolean healthy = resp.isSuccessful();
            log.info("[{}] 结果: {}", LogEvents.HEALTH_CHECK, healthy ? "UP" : "DOWN");
            return healthy;
            
        } catch (IOException e) {
            log.warn("[{}] 网络异常: {}", LogEvents.HEALTH_CHECK, e.getMessage());
            return false;
        }
    }
    
    /**
     * 详细健康状态
     * GET /health/detailed
     */
    public Map<String, Object> healthDetailed() {
        log.info("[{}] 详细健康检查", LogEvents.HEALTH_CHECK_DETAILED);
        
        HttpUrl url = HttpUrl.parse(config.getBaseUrl())
            .newBuilder()
            .addPathSegment("health")
            .addPathSegment("detailed")
            .build();
        
        Request request = new Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer " + config.getApiKey())
            .get()
            .build();
        
        try (Response resp = httpClient.newCall(request).execute()) {
            String body = resp.body().string();
            
            if (!resp.isSuccessful()) {
                log.warn("[{}] 失败: http={}, body={}", LogEvents.HEALTH_CHECK_DETAILED, resp.code(), body);
                throw new HermesApiException("HEALTH_DETAILED", resp.code(), body);
            }
            
            Map<String, Object> health = mapper.readValue(body, 
                new TypeReference<Map<String, Object>>() {});
            log.info("[{}] 成功", LogEvents.HEALTH_CHECK_DETAILED);
            return health;
            
        } catch (HermesException e) {
            throw e;
        } catch (IOException e) {
            log.error("[{}] IO 异常: {}", LogEvents.HEALTH_CHECK_DETAILED, e.getMessage());
            throw new HermesNetworkException("HEALTH_DETAILED", e);
        }
    }
}