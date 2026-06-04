package com.hermes.sdk.webhook;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WebhookConfig Builder 单元测试
 */
class WebhookConfigTest {

    @Test
    void testBuilderWithDefaults() {
        WebhookConfig config = WebhookConfig.builder()
            .baseUrl("http://localhost:8644")
            .secret("my-secret")
            .build();

        assertEquals("http://localhost:8644", config.getBaseUrl());
        assertEquals("my-secret", config.getSecret());
        assertEquals(5000, config.getConnectTimeoutMs());
        assertEquals(10000, config.getReadTimeoutMs());
        assertEquals(5000, config.getWriteTimeoutMs());
        assertEquals(2, config.getMaxRetries());
        assertEquals(1000, config.getRetryBackoffMs());
    }

    @Test
    void testBuilderEmptyBaseUrlThrows() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> WebhookConfig.builder().baseUrl("").secret("s").build());
        assertTrue(ex.getMessage().contains("baseUrl"));
    }

    @Test
    void testBuilderNullBaseUrlThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> WebhookConfig.builder().secret("s").build());
    }

    @Test
    void testBuilderEmptySecretThrows() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> WebhookConfig.builder().baseUrl("http://x").secret("").build());
        assertTrue(ex.getMessage().contains("secret"));
    }

    @Test
    void testBuilderNullSecretThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> WebhookConfig.builder().baseUrl("http://x").build());
    }

    @Test
    void testBuilderCustomTimeouts() {
        WebhookConfig config = WebhookConfig.builder()
            .baseUrl("http://x")
            .secret("s")
            .connectTimeoutMs(3000)
            .readTimeoutMs(15000)
            .writeTimeoutMs(7000)
            .maxRetries(5)
            .retryBackoffMs(500)
            .build();

        assertEquals(3000, config.getConnectTimeoutMs());
        assertEquals(15000, config.getReadTimeoutMs());
        assertEquals(7000, config.getWriteTimeoutMs());
        assertEquals(5, config.getMaxRetries());
        assertEquals(500, config.getRetryBackoffMs());
    }

    @Test
    void testBuilderTrimsTrailingSlash() {
        WebhookConfig config = WebhookConfig.builder()
            .baseUrl("http://localhost:8644/")
            .secret("s")
            .build();
        assertEquals("http://localhost:8644", config.getBaseUrl());
    }

    @Test
    void testBuilderNegativeMaxRetriesThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> WebhookConfig.builder()
                .baseUrl("http://x")
                .secret("s")
                .maxRetries(-1)
                .build());
    }

    @Test
    void testBuilderNegativeBackoffThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> WebhookConfig.builder()
                .baseUrl("http://x")
                .secret("s")
                .retryBackoffMs(-1)
                .build());
    }

    @Test
    void testBuilderNoRetryBackoff() {
        WebhookConfig config = WebhookConfig.builder()
            .baseUrl("http://x")
            .secret("s")
            .noRetryBackoff()
            .build();
        assertEquals(0, config.getRetryBackoffMs());
    }

    @Test
    void testBuilderZeroMaxRetriesAllowed() {
        WebhookConfig config = WebhookConfig.builder()
            .baseUrl("http://x")
            .secret("s")
            .maxRetries(0)
            .build();
        assertEquals(0, config.getMaxRetries());
    }
}
