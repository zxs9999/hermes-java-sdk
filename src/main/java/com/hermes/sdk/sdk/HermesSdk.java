package com.hermes.sdk.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hermes.sdk.config.HermesConfig;
import com.hermes.sdk.core.HermesApi;
import com.hermes.sdk.dto.Message;
import com.hermes.sdk.dto.Session;
import com.hermes.sdk.dto.Skill;
import com.hermes.sdk.dto.Toolset;
import com.hermes.sdk.exception.HermesException;
import com.hermes.sdk.logging.HermesLogger;
import com.hermes.sdk.logging.LogEvents;
import okhttp3.OkHttpClient;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;

/**
 * Hermes SDK — 业务封装层
 * 
 * 基于 HermesApi 原始 API，构建更易用的业务接口：
 * - 会话管理（带缓存、简化 API）
 * - Skill 执行（模板化）
 * - Toolset 查询（过滤、分类）
 * - 系统状态（健康、容量）
 * 
 * 分层：
 *   HermesClient（入口）
 *     └─> HermesApi（原始 API，一对一映射）
 *           └─> ApiServerCore（HTTP 底层）
 * 
 *     └─> HermesSdk（业务封装，简化 API）
 *           ├─ SessionManager   — 会话管理
 *           ├─ ToolsetManager  — Toolset 查询
 *           ├─ SkillExecutor    — Skill 执行
 *           └─ SystemMonitor   — 系统监控
 */
public class HermesSdk {
    
    private static final Logger log = HermesLogger.get(HermesSdk.class);
    
    private final HermesApi api;
    private final ObjectMapper mapper;
    
    // 子管理器
    private final SessionManager sessionManager;
    private final ToolsetManager toolsetManager;
    private final SkillExecutor skillExecutor;
    private final SystemMonitor systemMonitor;
    
    public HermesSdk(HermesConfig config, OkHttpClient httpClient, ObjectMapper mapper) {
        this.api = new HermesApi(config, httpClient, mapper);
        this.mapper = mapper;
        
        this.sessionManager = new SessionManager();
        this.toolsetManager = new ToolsetManager();
        this.skillExecutor = new SkillExecutor();
        this.systemMonitor = new SystemMonitor();
    }
    
    // ========== 子管理器 Getters ==========
    
    public SessionManager sessions() { return sessionManager; }
    public ToolsetManager toolsets() { return toolsetManager; }
    public SkillExecutor skills() { return skillExecutor; }
    public SystemMonitor system() { return systemMonitor; }
    
    // ========== 便捷入口 ==========
    
    /** 快速健康检查 */
    public boolean isHealthy() {
        return systemMonitor.isHealthy();
    }
    
    /** 列出所有可用 Skills（简化版）*/
    public List<Skill> listSkills() throws HermesException {
        return api.listSkills();
    }
    
    /** 列出所有 Toolsets（简化版）*/
    public List<Toolset> listToolsets() throws HermesException {
        return api.listToolsets();
    }
    
    // ==================== SessionManager ====================
    
    /**
     * 会话管理器
     * 
     * 简化会话操作，提供更高层次的抽象
     */
    public class SessionManager {
        
        private final Map<String, Session> sessionCache = new ConcurrentHashMap<>();
        
        /** 创建新会话 */
        public Session create(String title) throws HermesException {
            Session session = api.createSession(null, null, title, null);
            sessionCache.put(session.getSessionId(), session);
            log.info("[{}] 创建会话: id={}, title={}", LogEvents.SESSION_CREATE, 
                session.getSessionId(), title);
            return session;
        }
        
        /** 创建新会话（无标题）*/
        public Session create() throws HermesException {
            return create(null);
        }
        
        /** 获取会话（含缓存）*/
        public Session get(String sessionId) throws HermesException {
            Session cached = sessionCache.get(sessionId);
            if (cached != null) return cached;
            
            Session session = api.getSession(sessionId);
            sessionCache.put(sessionId, session);
            return session;
        }
        
        /** 列出最近会话 */
        public List<Session> listRecent(int limit) throws HermesException {
            return api.listSessions(limit, 0);
        }
        
        /** 删除会话（含缓存清理）*/
        public void delete(String sessionId) throws HermesException {
            api.deleteSession(sessionId);
            sessionCache.remove(sessionId);
            log.info("[{}] 删除会话: {}", LogEvents.SESSION_DELETE, sessionId);
        }
        
        /** 获取会话消息 */
        public List<Message> getMessages(String sessionId) throws HermesException {
            return api.getSessionMessages(sessionId);
        }
        
        /** 清除本地缓存 */
        public void clearCache() {
            sessionCache.clear();
        }
    }
    
    // ==================== ToolsetManager ====================
    
    /**
     * Toolset 管理器
     * 
     * 提供 Toolset 的查询、过滤、分类功能
     */
    public class ToolsetManager {
        
        private List<Toolset> cachedToolsets;
        private long cacheTime;
        private static final long CACHE_TTL_MS = 60_000; // 1 分钟
        
        /** 列出所有启用的 Toolsets */
        public List<Toolset> listEnabled() throws HermesException {
            return listAll().stream()
                .filter(Toolset::isEnabled)
                .collect(Collectors.toList());
        }
        
        /** 列出所有已配置的 Toolsets */
        public List<Toolset> listConfigured() throws HermesException {
            return listAll().stream()
                .filter(Toolset::isConfigured)
                .collect(Collectors.toList());
        }
        
        /** 按名称查找 Toolset */
        public Optional<Toolset> findByName(String name) throws HermesException {
            return listAll().stream()
                .filter(t -> t.getName().equals(name))
                .findFirst();
        }
        
        /** 获取 Toolset 包含的工具列表 */
        public List<String> getTools(String toolsetName) throws HermesException {
            return findByName(toolsetName)
                .map(Toolset::getTools)
                .orElse(Collections.emptyList());
        }
        
        /** 列出所有 Toolsets（含缓存）*/
        public List<Toolset> listAll() throws HermesException {
            if (cachedToolsets == null || isCacheExpired()) {
                cachedToolsets = api.listToolsets();
                cacheTime = System.currentTimeMillis();
            }
            return cachedToolsets;
        }
        
        /** 刷新缓存 */
        public void refresh() {
            cachedToolsets = null;
        }
        
        private boolean isCacheExpired() {
            return System.currentTimeMillis() - cacheTime > CACHE_TTL_MS;
        }
    }
    
    // ==================== SkillExecutor ====================
    
    /**
     * Skill 执行器
     * 
     * 提供 Skill 的查询、执行、状态管理
     * 注意：Skill 的实际执行通过 HermesClient.chat() + system prompt 实现
     */
    public class SkillExecutor {
        
        /** 列出所有可用的 Skills */
        public List<Skill> listAvailable() throws HermesException {
            return api.listSkills().stream()
                .filter(s -> s.isEnabled() || s.isEnabled())
                .collect(Collectors.toList());
        }
        
        /** 按名称查找 Skill */
        public Optional<Skill> findByName(String name) throws HermesException {
            return api.listSkills().stream()
                .filter(s -> s.getName().equals(name))
                .findFirst();
        }
        
        /** Skill 是否存在 */
        public boolean exists(String name) throws HermesException {
            return findByName(name).isPresent();
        }
    }
    
    // ==================== SystemMonitor ====================
    
    /**
     * 系统监控
     * 
     * 提供系统健康、容量、状态查询
     */
    public class SystemMonitor {
        
        private volatile boolean lastHealthState = false;
        private volatile long lastHealthCheck = 0;
        private static final long HEALTH_CACHE_TTL_MS = 10_000; // 10 秒
        
        /** 快速健康检查（带缓存）*/
        public boolean isHealthy() {
            if (System.currentTimeMillis() - lastHealthCheck < HEALTH_CACHE_TTL_MS) {
                return lastHealthState;
            }
            try {
                lastHealthState = api.health();
                lastHealthCheck = System.currentTimeMillis();
            } catch (HermesException e) {
                lastHealthState = false;
            }
            return lastHealthState;
        }
        
        /** 详细健康检查 */
        public Map<String, Object> getDetailedHealth() throws HermesException {
            return api.healthDetailed();
        }
        
        /** 获取 API 能力清单 */
        public Map<String, Object> getCapabilities() throws HermesException {
            return api.getCapabilities();
        }
        
        /** 获取可用模型列表 */
        public Map<String, Object> getModels() throws HermesException {
            return api.listModels();
        }
    }
}