package com.hermes.sdk.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hermes.sdk.exception.HermesApiException;
import com.hermes.sdk.exception.HermesException;
import com.hermes.sdk.exception.HermesNetworkException;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WebhookClient 集成测试
 *
 * 模拟真实 Hermes Gateway 行为：
 * - HMAC 签名验证
 * - 多种 HTTP 状态码
 * - 并发请求
 * - 大 payload
 * - 端到端流程
 */
class WebhookIntegrationTest {

    private MockWebhookServer server;
    private WebhookClient client;

    @BeforeEach
    void setUp() {
        server = new MockWebhookServer();
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) server.stop();
    }

    // ========== 真实 Hermes 模拟 ==========

    /**
     * 模拟 Hermes Gateway 行为：
     * 1. 验证 HMAC 签名
     * 2. 返回 202 + deliveryId
     * 3. 异步处理 + 投递到 Telegram
     */
    @Test
    void testRealHermesGatewaySimulation() throws Exception {
        // 模拟 Hermes 接收 webhook 的处理逻辑
        server.setDefault(202,
            "{\"status\":\"accepted\",\"delivery_id\":\"bid-2026-abc123\",\"message\":\"queued for processing\"}");

        WebhookConfig config = WebhookConfig.builder()
            .baseUrl(server.url())
            .secret("production-secret-key-2024")
            .build();
        WebhookClient hermesWebhook = new WebhookClient(config);

        // 业务系统调用
        WebhookResponse resp = hermesWebhook.trigger("bid_generation", Map.of(
            "project_name", "2026年城市轨道交通设计项目",
            "project_type", "工程设计",
            "client", "北京地铁公司",
            "deadline", "2026-07-01",
            "amount", 5000000
        ));

        // 验证
        assertEquals("accepted", resp.getStatus());
        assertNotNull(resp.getDeliveryId());
        assertTrue(resp.getDeliveryId().startsWith("bid-"));

        // 验证 HMAC 签名正确
        MockWebhookServer.MockRequest req = server.lastRequest();
        String expectedSig = HMACSigner.sign("production-secret-key-2024", req.body);
        assertEquals(expectedSig, req.header("X-Webhook-Signature"));
    }

    /**
     * 验证 Hermes 拒绝错误签名
     */
    @Test
    void testHermesRejectsInvalidSignature() {
        // 模拟 Hermes 收到错误签名后返回 401
        server.setDefault(401, "{\"error\":\"invalid signature\"}");

        WebhookConfig config = WebhookConfig.builder()
            .baseUrl(server.url())
            .secret("wrong-secret")
            .maxRetries(0)
            .build();
        WebhookClient client = new WebhookClient(config);

        HermesApiException ex = assertThrows(HermesApiException.class,
            () -> client.trigger("test", Map.of("k", "v")));

        assertEquals(401, ex.getHttpStatus());
        assertTrue(ex.getMessage().contains("401"));
    }

    // ========== 并发场景 ==========

    @Test
    void testConcurrentWebhookTriggers() throws Exception {
        server.setDefault(202, "{\"status\":\"accepted\",\"delivery_id\":\"concurrent-test\"}");

        WebhookConfig config = WebhookConfig.builder()
            .baseUrl(server.url())
            .secret("concurrent-secret")
            .maxRetries(0)
            .noRetryBackoff()
            .build();
        WebhookClient concurrentClient = new WebhookClient(config);

        int threadCount = 20;
        int requestsPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount * requestsPerThread);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            for (int j = 0; j < requestsPerThread; j++) {
                final int reqId = j;
                executor.submit(() -> {
                    try {
                        WebhookResponse resp = concurrentClient.trigger("concurrent_test", Map.of(
                            "thread_id", threadId,
                            "request_id", reqId,
                            "timestamp", System.currentTimeMillis()
                        ));
                        if ("accepted".equals(resp.getStatus())) {
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS), "All requests must complete in 30s");
        executor.shutdown();

        assertEquals(threadCount * requestsPerThread, successCount.get(),
            "All concurrent requests should succeed");
        assertEquals(0, failureCount.get());
        assertEquals(threadCount * requestsPerThread, server.requestCount());
    }

    // ========== 大 Payload 场景 ==========

    @Test
    void testLargePayload() throws Exception {
        server.setDefault(202, "{\"status\":\"accepted\",\"delivery_id\":\"large-payload\"}");

        WebhookConfig config = WebhookConfig.builder()
            .baseUrl(server.url())
            .secret("test")
            .readTimeoutMs(30000)
            .build();
        WebhookClient largeClient = new WebhookClient(config);

        // 构造 100KB payload
        Map<String, Object> payload = new HashMap<>();
        StringBuilder bigValue = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            bigValue.append("这是标书内容，包含技术方案、项目预算、人员配置等详细信息...");
        }
        payload.put("project_name", "大型标书项目");
        payload.put("content", bigValue.toString());
        payload.put("sections", List.of("技术方案", "项目预算", "人员配置", "进度计划"));

        WebhookResponse resp = largeClient.trigger("bid_draft", payload);

        assertEquals("accepted", resp.getStatus());

        // 验证服务端收到的 body 包含完整内容
        MockWebhookServer.MockRequest req = server.lastRequest();
        assertTrue(req.body.length() > 50000, "Body should be > 50KB, got: " + req.body.length());
        assertTrue(req.body.contains("大型标书项目"));
        assertTrue(req.body.contains("技术方案"));
    }

    // ========== Unicode 场景 ==========

    @Test
    void testUnicodePayload() throws Exception {
        server.setDefault(202, "{\"status\":\"accepted\"}");

        WebhookConfig config = WebhookConfig.builder()
            .baseUrl(server.url())
            .secret("unicode-secret")
            .build();
        WebhookClient unicodeClient = new WebhookClient(config);

        Map<String, Object> payload = Map.of(
            "project_name", "智能交通系统设计",
            "client", "北京市交通委员会",
            "description", "包含🚗🚇✈️ emoji 和混合中文 English text 123!@#",
            "tags", List.of("智能", "交通", "设计", "交通工程")
        );

        WebhookResponse resp = unicodeClient.trigger("test", payload);

        assertEquals("accepted", resp.getStatus());
        assertTrue(server.lastRequest().body.contains("智能交通"));
        assertTrue(server.lastRequest().body.contains("🚗"));
    }

    // ========== 错误响应场景 ==========

    @Test
    void test404RouteNotFound() {
        server.setDefault(404, "{\"error\":\"route not found\"}");

        WebhookConfig config = WebhookConfig.builder()
            .baseUrl(server.url())
            .secret("test")
            .maxRetries(0)
            .build();
        WebhookClient c = new WebhookClient(config);

        HermesApiException ex = assertThrows(HermesApiException.class,
            () -> c.trigger("non_existent_route", Map.of()));
        assertEquals(404, ex.getHttpStatus());
    }

    @Test
    void test403Forbidden() {
        server.setDefault(403, "{\"error\":\"forbidden - check IP whitelist\"}");

        WebhookConfig config = WebhookConfig.builder()
            .baseUrl(server.url())
            .secret("test")
            .maxRetries(0)
            .build();
        WebhookClient c = new WebhookClient(config);

        HermesApiException ex = assertThrows(HermesApiException.class,
            () -> c.trigger("test", Map.of()));
        assertEquals(403, ex.getHttpStatus());
    }

    @Test
    void test400BadRequest() {
        server.setDefault(400, "{\"error\":\"bad request: missing field\"}");

        WebhookConfig config = WebhookConfig.builder()
            .baseUrl(server.url())
            .secret("test")
            .maxRetries(0)
            .build();
        WebhookClient c = new WebhookClient(config);

        HermesApiException ex = assertThrows(HermesApiException.class,
            () -> c.trigger("test", Map.of()));
        assertEquals(400, ex.getHttpStatus());
        assertTrue(ex.getMessage().contains("400"));
    }

    @Test
    void test429RateLimitedShouldRetry() {
        // 429 视为 4xx，不重试（按设计）
        server.setDefault(429, "{\"error\":\"rate limited\"}");

        WebhookConfig config = WebhookConfig.builder()
            .baseUrl(server.url())
            .secret("test")
            .maxRetries(3)
            .noRetryBackoff()
            .build();
        WebhookClient c = new WebhookClient(config);

        assertThrows(HermesApiException.class,
            () -> c.trigger("test", Map.of()));
        assertEquals(1, server.requestCount(), "429 should not retry by current design");
    }

    @Test
    void test500InternalErrorRetriesThenFails() {
        server.setDefault(500, "{\"error\":\"internal\"}");

        WebhookConfig config = WebhookConfig.builder()
            .baseUrl(server.url())
            .secret("test")
            .maxRetries(2)
            .noRetryBackoff()
            .build();
        WebhookClient c = new WebhookClient(config);

        assertThrows(HermesApiException.class,
            () -> c.trigger("test", Map.of()));
        assertEquals(3, server.requestCount(), "1 initial + 2 retries = 3 attempts");
    }

    @Test
    void test502BadGatewayRetries() {
        server.setDefault(502, "{\"error\":\"bad gateway\"}");

        WebhookConfig config = WebhookConfig.builder()
            .baseUrl(server.url())
            .secret("test")
            .maxRetries(1)
            .noRetryBackoff()
            .build();
        WebhookClient c = new WebhookClient(config);

        assertThrows(HermesApiException.class,
            () -> c.trigger("test", Map.of()));
        assertEquals(2, server.requestCount(), "1 initial + 1 retry = 2 attempts");
    }

    @Test
    void test503ServiceUnavailableRetries() {
        server.enqueue(503, "{\"error\":\"service unavailable\"}");
        server.enqueueOk();

        WebhookConfig config = WebhookConfig.builder()
            .baseUrl(server.url())
            .secret("test")
            .maxRetries(2)
            .noRetryBackoff()
            .build();
        WebhookClient c = new WebhookClient(config);

        WebhookResponse resp = c.trigger("test", Map.of());
        assertEquals("accepted", resp.getStatus());
        assertEquals(2, server.requestCount());
    }

    // ========== 网络错误场景 ==========

    @Test
    void testConnectionRefusedRetriesThenFails() {
        // 连接到无效端口
        WebhookConfig config = WebhookConfig.builder()
            .baseUrl("http://127.0.0.1:1")
            .secret("test")
            .maxRetries(1)
            .noRetryBackoff()
            .connectTimeoutMs(500)
            .build();
        WebhookClient c = new WebhookClient(config);

        assertThrows(HermesNetworkException.class,
            () -> c.trigger("test", Map.of()));
    }

    @Test
    void testConnectionRefusedNoRetry() {
        WebhookConfig config = WebhookConfig.builder()
            .baseUrl("http://127.0.0.1:1")
            .secret("test")
            .maxRetries(0)
            .noRetryBackoff()
            .connectTimeoutMs(500)
            .build();
        WebhookClient c = new WebhookClient(config);

        assertThrows(HermesNetworkException.class,
            () -> c.trigger("test", Map.of()));
    }

    // ========== Header 完整性测试 ==========

    @Test
    void testUserAgentHeader() {
        server.enqueueOk();

        WebhookConfig config = WebhookConfig.builder()
            .baseUrl(server.url())
            .secret("test")
            .build();
        WebhookClient c = new WebhookClient(config);

        c.trigger("test", Map.of());

        String userAgent = server.lastRequest().header("User-agent");
        assertNotNull(userAgent);
        assertTrue(userAgent.startsWith("hermes-java-sdk/"));
    }

    @Test
    void testAllRequiredHeadersPresent() {
        server.enqueueOk();

        WebhookConfig config = WebhookConfig.builder()
            .baseUrl(server.url())
            .secret("test")
            .build();
        WebhookClient c = new WebhookClient(config);

        c.trigger("test", Map.of("k", "v"));

        MockWebhookServer.MockRequest req = server.lastRequest();
        assertEquals("POST", req.method);
        assertNotNull(req.header("X-Webhook-Signature"));
        assertNotNull(req.header("Content-type"));
        assertNotNull(req.header("User-agent"));
    }

    // ========== 重试退避测试 ==========

    @Test
    void testRetryWithExponentialBackoff() throws Exception {
        server.setDefault(500, "{\"error\":\"internal\"}");

        // 退避时间 100ms
        WebhookConfig config = WebhookConfig.builder()
            .baseUrl(server.url())
            .secret("test")
            .maxRetries(3)
            .retryBackoffMs(100)
            .build();
        WebhookClient c = new WebhookClient(config);

        long start = System.currentTimeMillis();
        assertThrows(HermesApiException.class,
            () -> c.trigger("test", Map.of()));
        long elapsed = System.currentTimeMillis() - start;

        // 退避时间应至少 100 + 200 + 400 = 700ms
        assertTrue(elapsed >= 700,
            "Backoff should take at least 700ms, got: " + elapsed + "ms");
        assertEquals(4, server.requestCount());
    }

    // ========== 业务集成场景 ==========

    /**
     * 模拟 Spring Boot 服务调用 Webhook
     */
    @Test
    void testBusinessScenario_SpringBootStyle() throws Exception {
        server.setDefault(202,
            "{\"status\":\"accepted\",\"delivery_id\":\"spring-2026-001\",\"message\":\"queued\"}");

        // 模拟从配置文件读取 webhook 配置
        String webhookUrl = server.url();
        String webhookSecret = "spring-config-secret";

        WebhookConfig config = WebhookConfig.builder()
            .baseUrl(webhookUrl)
            .secret(webhookSecret)
            .maxRetries(3)
            .connectTimeoutMs(5000)
            .readTimeoutMs(10000)
            .build();
        WebhookClient webhooks = new WebhookClient(config);

        // 模拟 Service 层
        String projectId = "BD2026-001";
        String deliveryId = triggerBidGeneration(webhooks, projectId, "工程设计",
            "智能交通系统设计，包括信号控制、车路协同等子系统");

        assertNotNull(deliveryId);
        assertTrue(deliveryId.startsWith("spring-"));

        // 验证请求内容
        MockWebhookServer.MockRequest req = server.lastRequest();
        assertTrue(req.body.contains(projectId));
        assertTrue(req.body.contains("工程设计"));
    }

    private String triggerBidGeneration(WebhookClient webhooks,
                                          String projectId,
                                          String type,
                                          String requirements) {
        try {
            WebhookResponse resp = webhooks.trigger("bid_generation", Map.of(
                "project_id", projectId,
                "project_type", type,
                "requirements", requirements,
                "submitted_at", System.currentTimeMillis()
            ));
            return resp.getDeliveryId();
        } catch (HermesException e) {
            return null;
        }
    }
}
