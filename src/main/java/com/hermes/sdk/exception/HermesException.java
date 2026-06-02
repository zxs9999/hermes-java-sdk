package com.hermes.sdk.exception;

/**
 * Hermes SDK 异常基类
 */
public class HermesException extends RuntimeException {
    
    private final String errorCode;
    private final int httpStatus;
    
    public HermesException(String message) {
        super(message);
        this.errorCode = "UNKNOWN";
        this.httpStatus = -1;
    }
    
    public HermesException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "UNKNOWN";
        this.httpStatus = -1;
    }
    
    public HermesException(String errorCode, String message, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    
    public HermesException(String errorCode, String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public int getHttpStatus() {
        return httpStatus;
    }
    
    @Override
    public String toString() {
        return String.format("HermesException[%s, status=%d]: %s", errorCode, httpStatus, getMessage());
    }
}