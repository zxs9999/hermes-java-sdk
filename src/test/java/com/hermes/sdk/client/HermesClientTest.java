package com.hermes.sdk.client;

import com.hermes.sdk.config.HermesConfig;
import com.hermes.sdk.exception.HermesAuthException;
import com.hermes.sdk.exception.HermesNetworkException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HermesClient 单元测试
 */
class HermesClientTest {
    
    @Test
    void testBuilder() {
        HermesClient client = HermesClient.builder()
            .baseUrl("https://api.hermes.com")
            .apiKey("sk-test-key-123")
            .model("gpt-4")
            .connectTimeout(30)
            .readTimeout(180)
            .temperature(0.7)
            .maxTokens(4096)
            .maxRetries(3)
            .requireHttps(true)
            .build();
        
        assertEquals("https://api.hermes.com", client.getBaseUrl());
        assertEquals("gpt-4", client.getModel());
    }
    
    @Test
    void testBuilderDefaultValues() {
        HermesClient client = HermesClient.builder()
            .baseUrl("https://api.hermes.com")
            .apiKey("sk-test")
            .build();
        
        assertEquals("http://localhost:8080", client.getBaseUrl());
        assertEquals("gpt-4", client.getModel());
    }
    
    @Test
    void testBuilderRequireHttps() {
        assertThrows(SecurityException.class, () -> {
            HermesClient.builder()
                .baseUrl("http://insecure.com")
                .apiKey("sk-test")
                .requireHttps(true)
                .build();
        });
    }
    
    @Test
    void testChatEmptyMessage() {
        HermesClient client = HermesClient.builder()
            .baseUrl("https://api.hermes.com")
            .apiKey("sk-test")
            .build();
        
        assertThrows(IllegalArgumentException.class, () -> {
            client.chat("");
        });
    }
    
    @Test
    void testChatNullMessage() {
        HermesClient client = HermesClient.builder()
            .baseUrl("https://api.hermes.com")
            .apiKey("sk-test")
            .build();
        
        assertThrows(IllegalArgumentException.class, () -> {
            client.chat(null);
        });
    }
    
    @Test
    void testActivateSkillEmptyName() {
        HermesClient client = HermesClient.builder()
            .baseUrl("https://api.hermes.com")
            .apiKey("sk-test")
            .build();
        
        assertThrows(IllegalArgumentException.class, () -> {
            client.activateSkill("", "task");
        });
    }
    
    @Test
    void testActivateSkillNullTask() {
        HermesClient client = HermesClient.builder()
            .baseUrl("https://api.hermes.com")
            .apiKey("sk-test")
            .build();
        
        assertThrows(IllegalArgumentException.class, () -> {
            client.activateSkill("skill-name", null);
        });
    }
    
    @Test
    void testNewSession() {
        HermesClient client = HermesClient.builder()
            .baseUrl("https://api.hermes.com")
            .apiKey("sk-test")
            .build();
        
        assertNotNull(client.newSession());
        assertNotNull(client.newThreadSafeSession());
    }
}