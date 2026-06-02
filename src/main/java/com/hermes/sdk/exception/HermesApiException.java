package com.hermes.sdk.exception;

/**
 * API 异常 - Hermes 返回错误
 */
public class HermesApiException extends HermesException {
    
    private final String errorDetail;
    
    public HermesApiException(String message, int httpStatus) {
        super("API_ERROR", message, httpStatus);
        this.errorDetail = null;
    }
    
    public HermesApiException(String errorCode, String message, int httpStatus, String errorDetail) {
        super(errorCode, message, httpStatus);
        this.errorDetail = errorDetail;
    }
    
    public HermesApiException(String message, int httpStatus, Throwable cause) {
        super("API_ERROR", message, httpStatus, cause);
        this.errorDetail = null;
    }
    
    public String getErrorDetail() {
        return errorDetail;
    }
}