package com.hermes.sdk.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hermes.sdk.config.HermesConfig;
import com.hermes.sdk.dto.Session;
import com.hermes.sdk.exception.HermesApiException;
import com.hermes.sdk.exception.HermesNetworkException;
import com.hermes.sdk.logging.HermesLogger;
import com.hermes.sdk.logging.LogEvents;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Session 管理服务
 * 
 * 对应 Hermes API:
 * - GET    /api/sessions              - 列出所有会话
 * - POST   /api/sessions              - 创建空白会话
 * - GET    /api/sessions/{id}         - 获取会话
 * - DELETE /api/sessions/{id}         - 删除会话
 * - GET    /api/sessions/{id}/messages - 获取会话消息历史
 */
public class SessionService {
    
    private static final Logger log = HermesLogger.get(SessionService.class);
    
    private final HermesConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper mapper;
    
    public SessionService(HermesConfig config, OkHttpClient httpClient, ObjectMapper mapper) {
        this.config = config;
        this.httpClient = httpClient;
        this.mapper = mapper;
    }
    
    /**
     * 列出所有会话
     * 
     * @param limit 返回数量（默认 20）
     * @param offset 偏移量
     * @return 会话列表
     */
    public List<Session> list(int limit, int offset) {
        log.info("[{}] limit={}, offset={}", LogEvents.SESSION_LIST, limit, offset);
        HttpUrl url = HttpUrl.parse(config.getBaseUrl())
            .newBuilder()
            .addPathSegment("api")
            .addPathSegment("sessions")
            .addQueryParameter("limit", String.valueOf(limit))
            .addQueryParameter("offset", String.valueOf(offset))
            .build();
        
        Request request = new Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer " + config.getApiKey())
            .get()
            .build();
        
        try (Response resp = httpClient.newCall(request).execute()) {
            String body = resp.body().string();
            
            if (!resp.isSuccessful()) {
                log.error("[{}] 失败: http={}, body={}", LogEvents.SESSION_LIST, resp.code(), body);
                throw new HermesApiException("SESSION_LIST", resp.code(), body);
            }
            
            List<Session> sessions = mapper.readValue(body, new TypeReference<List<Session>>() {});
            log.info("[{}] 成功: count={}", LogEvents.SESSION_LIST, sessions.size());
            return sessions;
            
        } catch (HermesException e) {
            throw e;
        } catch (IOException e) {
            log.error("[{}] IO 异常: {}", LogEvents.SESSION_LIST, e.getMessage());
            throw new HermesNetworkException("SESSION_LIST", e);
        }
    }
    
    /**
     * 列出所有会话（默认 20 条）
     */
    public List<Session> list() {
        return list(20, 0);
    }
    
    /**
     * 获取单个会话
     */
    public Session get(String sessionId) {
        log.info("[{}] sessionId={}", LogEvents.SESSION_GET, sessionId);
        HttpUrl url = HttpUrl.parse(config.getBaseUrl())
            .newBuilder()
            .addPathSegment("api")
            .addPathSegment("sessions")
            .addPathSegment(sessionId)
            .build();
        
        Request request = new Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer " + config.getApiKey())
            .get()
            .build();
        
        try (Response resp = httpClient.newCall(request).execute()) {
            String body = resp.body().string();
            
            if (!resp.isSuccessful()) {
                log.error("[{}] 失败: http={}, body={}", LogEvents.SESSION_GET, resp.code(), body);
                throw new HermesApiException("SESSION_GET", resp.code(), body);
            }
            
            return mapper.readValue(body, Session.class);
            
        } catch (HermesException e) {
            throw e;
        } catch (IOException e) {
            log.error("[{}] IO 异常: {}", LogEvents.SESSION_GET, e.getMessage());
            throw new HermesNetworkException("SESSION_GET", e);
        }
    }
    
    /**
     * 创建空白会话
     */
    public Session create() {
        log.info("[{}] 创建空白会话", LogEvents.SESSION_CREATE);
        HttpUrl url = HttpUrl.parse(config.getBaseUrl())
            .newBuilder()
            .addPathSegment("api")
            .addPathSegment("sessions")
            .build();
        
        Request request = new Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer " + config.getApiKey())
            .post(RequestBody.create("", MediaType.parse("application/json")))
            .build();
        
        try (Response resp = httpClient.newCall(request).execute()) {
            String body = resp.body().string();
            
            if (!resp.isSuccessful()) {
                log.error("[{}] 失败: http={}, body={}", LogEvents.SESSION_CREATE, resp.code(), body);
                throw new HermesApiException("SESSION_CREATE", resp.code(), body);
            }
            
            Session session = mapper.readValue(body, Session.class);
            log.info("[{}] 成功: sessionId={}", LogEvents.SESSION_CREATE, session.getSessionId());
            return session;
            
        } catch (HermesException e) {
            throw e;
        } catch (IOException e) {
            log.error("[{}] IO 异常: {}", LogEvents.SESSION_CREATE, e.getMessage());
            throw new HermesNetworkException("SESSION_CREATE", e);
        }
    }
    
    /**
     * 删除会话
     */
    public void delete(String sessionId) {
        log.info("[{}] sessionId={}", LogEvents.SESSION_DELETE, sessionId);
        HttpUrl url = HttpUrl.parse(config.getBaseUrl())
            .newBuilder()
            .addPathSegment("api")
            .addPathSegment("sessions")
            .addPathSegment(sessionId)
            .build();
        
        Request request = new Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer " + config.getApiKey())
            .delete()
            .build();
        
        try (Response resp = httpClient.newCall(request).execute()) {
            String body = resp.body().string();
            
            if (!resp.isSuccessful()) {
                log.error("[{}] 失败: http={}, body={}", LogEvents.SESSION_DELETE, resp.code(), body);
                throw new HermesApiException("SESSION_DELETE", resp.code(), body);
            }
            
            log.info("[{}] 成功: sessionId={}", LogEvents.SESSION_DELETE, sessionId);
            
        } catch (HermesException e) {
            throw e;
        } catch (IOException e) {
            log.error("[{}] IO 异常: {}", LogEvents.SESSION_DELETE, e.getMessage());
            throw new HermesNetworkException("SESSION_DELETE", e);
        }
    }
    
    /**
     * 获取会话消息历史
     */
    public List<Map<String, Object>> getMessages(String sessionId) {
        log.info("[{}] sessionId={}", LogEvents.SESSION_MESSAGES, sessionId);
        HttpUrl url = HttpUrl.parse(config.getBaseUrl())
            .newBuilder()
            .addPathSegment("api")
            .addPathSegment("sessions")
            .addPathSegment(sessionId)
            .addPathSegment("messages")
            .build();
        
        Request request = new Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer " + config.getApiKey())
            .get()
            .build();
        
        try (Response resp = httpClient.newCall(request).execute()) {
            String body = resp.body().string();
            
            if (!resp.isSuccessful()) {
                log.error("[{}] 失败: http={}, body={}", LogEvents.SESSION_MESSAGES, resp.code(), body);
                throw new HermesApiException("SESSION_MESSAGES", resp.code(), body);
            }
            
            List<Map<String, Object>> messages = mapper.readValue(body, 
                new TypeReference<List<Map<String, Object>>>() {});
            log.info("[{}] 成功: count={}", LogEvents.SESSION_MESSAGES, messages.size());
            return messages;
            
        } catch (HermesException e) {
            throw e;
        } catch (IOException e) {
            log.error("[{}] IO 异常: {}", LogEvents.SESSION_MESSAGES, e.getMessage());
            throw new HermesNetworkException("SESSION_MESSAGES", e);
        }
    }
}