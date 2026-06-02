package com.hermes.sdk.exception;

/**
 * 认证异常 - 不可恢复，API Key 问题
 */
public class HermesAuthException extends HermesException {
    
    public HermesAuthException(String message) {
        super("AUTH_ERROR", message, 401);
    }
    
    public HermesAuthException(String message, Throwable cause) {
        super("AUTH_ERROR", message, 401, cause);
    }
}