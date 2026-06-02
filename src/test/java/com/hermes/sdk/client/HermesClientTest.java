package com.hermes.sdk.client;

import com.hermes.sdk.config.HermesConfig;
import com.hermes.sdk.transport.TransportType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HermesClient 单元测试
 */
class HermesClientTest {
    
    @Test
    void testBuilderDefaultValues() {
        HermesClient client = HermesClient.builder()
            .baseUrl("https://api.hermes.com")
            .build();
        
        assertEquals("https://api.hermes.com", client.getBaseUrl());
        assertEquals(TransportType.HTTP, client.getConfig().getTransportType());
        assertEquals(30, client.getConfig().getConnectTimeout());
        assertEquals(180, client.getConfig().getReadTimeout());
        assertEquals(3, client.getConfig().getMaxRetries());
    }
    
    @Test
    void testBuilderCustomValues() {
        HermesClient client = HermesClient.builder()
            .baseUrl("https://api.hermes.com")
            .transportType(TransportType.HTTP)
            .connectTimeout(60)
            .readTimeout(300)
            .maxRetries(5)
            .build();
        
        assertEquals("https://api.hermes.com", client.getBaseUrl());
        assertEquals(60, client.getConfig().getConnectTimeout());
        assertEquals(300, client.getConfig().getReadTimeout());
        assertEquals(5, client.getConfig().getMaxRetries());
    }
    
    @Test
    void testChatEmptyMessage() {
        HermesClient client = HermesClient.builder()
            .baseUrl("https://api.hermes.com")
            .build();
        
        assertThrows(IllegalArgumentException.class, () -> {
            client.chat("");
        });
    }
    
    @Test
    void testChatNullMessage() {
        HermesClient client = HermesClient.builder()
            .baseUrl("https://api.hermes.com")
            .build();
        
        assertThrows(IllegalArgumentException.class, () -> {
            client.chat(null);
        });
    }
    
    @Test
    void testChatWithSystemPromptEmptyMessage() {
        HermesClient client = HermesClient.builder()
            .baseUrl("https://api.hermes.com")
            .build();
        
        assertThrows(IllegalArgumentException.class, () -> {
            client.chatWithSystemPrompt("You are helpful", "");
        });
    }
    
    @Test
    void testChatWithSystemPromptNullMessage() {
        HermesClient client = HermesClient.builder()
            .baseUrl("https://api.hermes.com")
            .build();
        
        assertThrows(IllegalArgumentException.class, () -> {
            client.chatWithSystemPrompt("You are helpful", null);
        });
    }
    
    @Test
    void testActivateSkillEmptyName() {
        HermesClient client = HermesClient.builder()
            .baseUrl("https://api.hermes.com")
            .build();
        
        assertThrows(IllegalArgumentException.class, () -> {
            client.activateSkill("", "task");
        });
    }
    
    @Test
    void testActivateSkillNullTask() {
        HermesClient client = HermesClient.builder()
            .baseUrl("https://api.hermes.com")
            .build();
        
        assertThrows(IllegalArgumentException.class, () -> {
            client.activateSkill("skill-name", null);
        });
    }
    
    @Test
    void testActivateSkillEmptyTask() {
        HermesClient client = HermesClient.builder()
            .baseUrl("https://api.hermes.com")
            .build();
        
        assertThrows(IllegalArgumentException.class, () -> {
            client.activateSkill("skill-name", "");
        });
    }
    
    @Test
    void testHealthCheck() {
        HermesClient client = HermesClient.builder()
            .baseUrl("http://localhost:9999")
            .connectTimeout(1)
            .readTimeout(1)
            .build();
        
        // localhost:9999 不存在，healthCheck 返回 false
        assertFalse(client.healthCheck());
    }
    
    @Test
    void testGetConfig() {
        HermesClient client = HermesClient.builder()
            .baseUrl("https://api.hermes.com")
            .transportType(TransportType.HTTP)
            .connectTimeout(45)
            .readTimeout(120)
            .maxRetries(2)
            .build();
        
        HermesConfig config = client.getConfig();
        assertEquals("https://api.hermes.com", config.getBaseUrl());
        assertEquals(TransportType.HTTP, config.getTransportType());
        assertEquals(45, config.getConnectTimeout());
        assertEquals(120, config.getReadTimeout());
        assertEquals(2, config.getMaxRetries());
    }
}