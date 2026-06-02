package com.hermes.sdk.exception;

/**
 * 网络异常 - 可恢复
 */
public class HermesNetworkException extends HermesException {
    
    public HermesNetworkException(String message) {
        super("NETWORK_ERROR", message, -1);
    }
    
    public HermesNetworkException(String message, Throwable cause) {
        super("NETWORK_ERROR", message, -1, cause);
    }
}