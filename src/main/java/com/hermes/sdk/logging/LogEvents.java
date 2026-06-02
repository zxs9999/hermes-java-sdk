package com.hermes.sdk.logging;

/**
 * 日志事件常量
 * 
 * 统一日志格式和事件名称，便于日志聚合和检索
 */
public final class LogEvents {
    
    private LogEvents() {}
    
    // ========== HermesClient ==========
    public static final String CHAT_REQUEST  = "HERMES_CHAT_REQUEST";
    public static final String CHAT_RESPONSE = "HERMES_CHAT_RESPONSE";
    public static final String CHAT_ERROR    = "HERMES_CHAT_ERROR";
    public static final String HEALTH_CHECK  = "HERMES_HEALTH_CHECK";
    
    // ========== SkillService ==========
    public static final String SKILL_ACTIVATE   = "HERMES_SKILL_ACTIVATE";
    public static final String SKILL_ASYNC_START = "HERMES_SKILL_ASYNC_START";
    public static final String SKILL_ASYNC_DONE  = "HERMES_SKILL_ASYNC_DONE";
    public static final String SKILL_ASYNC_ERROR = "HERMES_SKILL_ASYNC_ERROR";
    
    // ========== ChatSession ==========
    public static final String SESSION_CREATE = "HERMES_SESSION_CREATE";
    public static final String SESSION_CLEAR   = "HERMES_SESSION_CLEAR";
    
    // ========== Config ==========
    public static final String CONFIG_INIT   = "HERMES_CONFIG_INIT";
    public static final String CONFIG_ERROR  = "HERMES_CONFIG_ERROR";
    
    // ========== Retry ==========
    public static final String RETRY_NETWORK = "HERMES_RETRY_NETWORK";
    public static final String RETRY_API      = "HERMES_RETRY_API";
    
    // ========== HTTP ==========
    public static final String HTTP_REQUEST  = "HERMES_HTTP_REQUEST";
    public static final String HTTP_RESPONSE = "HERMES_HTTP_RESPONSE";
    public static final String HTTP_ERROR    = "HERMES_HTTP_ERROR";
    
    // ========== SessionService (API Server) ==========
    public static final String SESSION_LIST      = "HERMES_SESSION_LIST";
    public static final String SESSION_GET       = "HERMES_SESSION_GET";
    public static final String SESSION_CREATE    = "HERMES_SESSION_CREATE";
    public static final String SESSION_DELETE    = "HERMES_SESSION_DELETE";
    public static final String SESSION_MESSAGES  = "HERMES_SESSION_MESSAGES";
    
    // ========== RunService (API Server) ==========
    public static final String RUN_START   = "HERMES_RUN_START";
    public static final String RUN_STATUS  = "HERMES_RUN_STATUS";
    public static final String RUN_EVENTS  = "HERMES_RUN_EVENTS";
    public static final String RUN_APPROVAL = "HERMES_RUN_APPROVAL";
    public static final String RUN_STOP    = "HERMES_RUN_STOP";
    
    // ========== ApiServerService ==========
    public static final String SKILLS_LIST       = "HERMES_SKILLS_LIST";
    public static final String TOOLSETS_LIST     = "HERMES_TOOLSETS_LIST";
    public static final String CAPABILITIES_GET  = "HERMES_CAPABILITIES_GET";
    public static final String MODELS_LIST       = "HERMES_MODELS_LIST";
    public static final String HEALTH_CHECK_DETAILED = "HERMES_HEALTH_DETAILED";
}