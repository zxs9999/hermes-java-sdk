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
}