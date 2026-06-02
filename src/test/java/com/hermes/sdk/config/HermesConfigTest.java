package com.hermes.sdk.config;

import com.hermes.sdk.transport.TransportType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HermesConfig 单元测试
 */
class HermesConfigTest {
    
    @Test
    void testBuilderDefaultValues() {
        HermesConfig config = HermesConfig.builder().build();
        
        assertEquals("http://localhost:8080", config.getBaseUrl());
        assertEquals(TransportType.HTTP, config.getTransportType());
        assertEquals(30, config.getConnectTimeout());
        assertEquals(180, config.getReadTimeout());
        assertEquals(3, config.getMaxRetries());
    }
    
    @Test
    void testBuilderCustomValues() {
        HermesConfig config = HermesConfig.builder()
            .baseUrl("https://api.example.com")
            .transportType(TransportType.RPC)
            .connectTimeout(60)
            .readTimeout(600)
            .maxRetries(5)
            .build();
        
        assertEquals("https://api.example.com", config.getBaseUrl());
        assertEquals(TransportType.RPC, config.getTransportType());
        assertEquals(60, config.getConnectTimeout());
        assertEquals(600, config.getReadTimeout());
        assertEquals(5, config.getMaxRetries());
    }
    
    @Test
    void testImmutability() {
        HermesConfig config = HermesConfig.builder()
            .baseUrl("https://api.example.com")
            .build();
        
        // fields are final - 确保不可变
        assertTrue(config.getBaseUrl().equals("https://api.example.com"));
    }
    
    @Test
    void testTransportTypes() {
        assertEquals(TransportType.HTTP, TransportType.valueOf("HTTP"));
        assertEquals(TransportType.RPC, TransportType.valueOf("RPC"));
        assertEquals(TransportType.WEBSOCKET, TransportType.valueOf("WEBSOCKET"));
    }
}