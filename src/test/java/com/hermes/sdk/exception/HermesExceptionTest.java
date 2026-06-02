package com.hermes.sdk.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HermesExceptionTest {
    
    @Test
    void testBaseException() {
        HermesException e = new HermesException("NETWORK_ERROR", "网络错误", 500);
        assertEquals("NETWORK_ERROR", e.getErrorCode());
        assertEquals(500, e.getHttpStatus());
        assertEquals("网络错误", e.getMessage());
    }
    
    @Test
    void testBaseExceptionWithCause() {
        RuntimeException cause = new RuntimeException("original");
        HermesException e = new HermesException("API_ERROR", "API错误", 400, cause);
        assertEquals(cause, e.getCause());
    }
    
    @Test
    void testNetworkException() {
        HermesNetworkException e = new HermesNetworkException("连接超时");
        assertEquals("NETWORK_ERROR", e.getErrorCode());
        assertEquals(-1, e.getHttpStatus());
    }
    
    @Test
    void testAuthException() {
        HermesAuthException e = new HermesAuthException("API Key无效");
        assertEquals("AUTH_ERROR", e.getErrorCode());
        assertEquals(401, e.getHttpStatus());
    }
    
    @Test
    void testApiException() {
        HermesApiException e = new HermesApiException("请求失败", 500);
        assertEquals("API_ERROR", e.getErrorCode());
        assertEquals(500, e.getHttpStatus());
    }
    
    @Test
    void testApiExceptionWithDetail() {
        HermesApiException e = new HermesApiException("INVALID_PARAM", "参数错误", 400, "field 'name' is required");
        assertEquals("INVALID_PARAM", e.getErrorCode());
        assertEquals(400, e.getHttpStatus());
        assertEquals("field 'name' is required", e.getErrorDetail());
    }
}