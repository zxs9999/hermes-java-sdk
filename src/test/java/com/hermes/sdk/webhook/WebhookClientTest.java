package com.hermes.sdk.webhook;

import com.hermes.sdk.exception.HermesApiException;
import com.hermes.sdk.exception.HermesNetworkException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WebhookClient 单元测试
 */
class WebhookClientTest {

    private MockWebhookServer server;
    private WebhookClient client;

    @BeforeEach
    void setUp() {
        server = new MockWebhookServer();
        server.start();
        WebhookConfig config = WebhookConfig.builder()
            .baseUrl(server.url())
            .secret("test-secret-2024")
            .maxRetries(2)
            .noRetryBackoff()
            .build();
        client = new WebhookClient(config);
    }

    @AfterEach
    void tearDown() {
        if (server != null) server.stop();
    }

    @Test
    void testTriggerSuccess() {
        server.enqueueOk();

        WebhookResponse resp = client.trigger("bid_draft", Map.of(
            "project_name", "BD2010",
            "project_type", "设计"
        ));

        assertNotNull(resp);
        assertEquals("accepted", resp.getStatus());
        assertEquals("wh-default", resp.getDeliveryId());
    }

    @Test
    void testTriggerWithHMACSignatureHeader() {
        server.enqueueOk();

        client.trigger("test_route", Map.of("k", "v"));

        MockWebhookServer.MockRequest req = server.lastRequest();
        assertNotNull(req);
        String sig = req.header("X-Webhook-Signature");
        assertNotNull(sig, "X-Webhook-Signature header must be present");
        assertEquals(64, sig.length(), "SHA-256 hex must be 64 chars");
        assertTrue(sig.matches("[0-9a-f]+"), "Signature must be lowercase hex");
    }

    @Test
    void testTriggerSendsCorrectPath() {
        server.enqueueOk();

        client.trigger("my_route", Map.of());

        assertEquals("/webhooks/my_route", server.lastRequest().path);
    }

    @Test
    void testTriggerSendsCorrectBody() throws Exception {
        server.enqueueOk();

        Map<String, Object> payload = new HashMap<>();
        payload.put("project_name", "BD2010");
        payload.put("amount", 1000);
        client.trigger("test", payload);

        String body = server.lastRequest().body;
        assertTrue(body.contains("\"project_name\":\"BD2010\""), "Body should contain project_name: " + body);
        assertTrue(body.contains("\"amount\":1000"), "Body should contain amount: " + body);
    }

    @Test
    void testTriggerSendsContentTypeHeader() {
        server.enqueueOk();

        client.trigger("test", Map.of());

        String contentType = server.lastRequest().header("Content-type");
        assertNotNull(contentType);
        assertTrue(contentType.toLowerCase().contains("application/json"));
    }

    @Test
    void testTrigger4xxDoesNotRetry() {
        server.enqueueUnauthorized();
        server.enqueueOk(); // 不应被消费

        HermesApiException ex = assertThrows(HermesApiException.class,
            () -> client.trigger("test", Map.of("k", "v")));

        assertTrue(ex.getMessage().contains("401"));
        assertEquals(1, server.requestCount(), "4xx should not retry");
    }

    @Test
    void testTrigger5xxRetriesUntilSuccess() {
        server.enqueueInternalError();
        server.enqueueInternalError();
        server.enqueueOk();

        WebhookResponse resp = client.trigger("test", Map.of("k", "v"));

        assertNotNull(resp);
        assertEquals("accepted", resp.getStatus());
        assertEquals(3, server.requestCount(), "Should retry twice then succeed");
    }

    @Test
    void testTrigger5xxRetriesExhaustedThrows() {
        server.enqueueInternalError();
        server.enqueueInternalError();
        server.enqueueInternalError();
        server.enqueueInternalError();

        HermesApiException ex = assertThrows(HermesApiException.class,
            () -> client.trigger("test", Map.of("k", "v")));

        assertTrue(ex.getMessage().contains("retries") || ex.getMessage().contains("500"));
        // maxRetries=2: 1 初始 + 2 重试 = 3 次请求
        assertEquals(3, server.requestCount(), "Should make initial + 2 retry attempts");
    }

    @Test
    void testTriggerEmptyRouteNameThrows() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> client.trigger("", Map.of("k", "v")));
        assertTrue(ex.getMessage().contains("routeName"));
    }

    @Test
    void testTriggerWhitespaceRouteNameThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> client.trigger("   ", Map.of("k", "v")));
    }

    @Test
    void testTriggerNullRouteNameThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> client.trigger(null, Map.of("k", "v")));
    }

    @Test
    void testTriggerNullPayloadThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> client.trigger("test", null));
    }

    @Test
    void testTriggerIOExceptionFailsWithNetworkException() {
        WebhookConfig badConfig = WebhookConfig.builder()
            .baseUrl("http://127.0.0.1:1") // 无效端口
            .secret("test")
            .maxRetries(0)
            .noRetryBackoff()
            .build();
        WebhookClient badClient = new WebhookClient(badConfig);

        assertThrows(HermesNetworkException.class,
            () -> badClient.trigger("test", Map.of("k", "v")));
    }

    @Test
    void testTriggerParsesDeliveryIdFromResponse() {
        server.setDefault(202, "{\"status\":\"accepted\",\"delivery_id\":\"wh-12345\",\"message\":\"queued\"}");

        WebhookResponse resp = client.trigger("test", Map.of("k", "v"));

        assertEquals("accepted", resp.getStatus());
        assertEquals("wh-12345", resp.getDeliveryId());
        assertEquals("queued", resp.getMessage());
    }

    @Test
    void testTriggerHandlesEmptyResponseBody() {
        server.setDefault(202, "");

        WebhookResponse resp = client.trigger("test", Map.of("k", "v"));

        assertEquals("accepted", resp.getStatus());
        assertNull(resp.getDeliveryId());
    }
}
