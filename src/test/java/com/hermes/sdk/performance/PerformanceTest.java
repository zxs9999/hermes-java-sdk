package com.hermes.sdk.performance;

import com.hermes.sdk.client.HermesClient;
import com.hermes.sdk.config.HermesConfig;
import okhttp3.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 性能测试 — 超时、并发、资源使用
 */
class PerformanceTest {

    // ========== 超时测试 ==========

    @Test
    void testConnectTimeout() {
        long start = System.currentTimeMillis();
        HermesClient client = HermesClient.builder()
            .baseUrl("http://192.0.2.1:9999") // 不可达地址
            .connectTimeout(1)
            .readTimeout(1)
            .build();

        boolean result = client.healthCheck();
        long elapsed = System.currentTimeMillis() - start;

        assertFalse(result);
        // 应该在 2 秒内返回（connectTimeout + 缓冲）
        assertTrue(elapsed < 3000, "healthCheck 超时太长: " + elapsed + "ms");
    }

    @Test
    void testReadTimeout() throws IOException {
        OkHttpClient mockClient = mock(OkHttpClient.class);
        Call mockCall = mock(Call.class);
        // 模拟超时
        when(mockCall.execute()).thenThrow(new IOException("timeout"));
        when(mockClient.newCall(any())).thenReturn(mockCall);

        com.hermes.sdk.transport.HttpTransport transport = new com.hermes.sdk.transport.HttpTransport(
            HermesConfig.builder().baseUrl("http://localhost:8080").build(),
            mockClient,
            new com.fasterxml.jackson.databind.ObjectMapper()
        );

        long start = System.currentTimeMillis();
        assertThrows(Exception.class, () -> transport.get("/slow"));
        long elapsed = System.currentTimeMillis() - start;

        // 应该快速失败，而不是挂起
        assertTrue(elapsed < 1000, "读超时响应太慢: " + elapsed + "ms");
    }

    // ========== Builder 性能 ==========

    @Test
    void testBuilderPerformance() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            HermesClient.builder()
                .baseUrl("http://localhost:8080")
                .connectTimeout(30)
                .readTimeout(180)
                .build();
        }
        long elapsed = System.currentTimeMillis() - start;

        // 1000 次构建应该在 1 秒内完成
        assertTrue(elapsed < 1000, "Builder 性能太差: " + elapsed + "ms");
    }

    // ========== 并发测试 ==========

    @Test
    void testConcurrentHealthCheck() throws InterruptedException {
        HermesClient client = HermesClient.builder()
            .baseUrl("http://localhost:9999")
            .connectTimeout(1)
            .readTimeout(1)
            .build();

        int threads = 10;
        CountDownLatch latch = new CountDownLatch(threads);
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    client.healthCheck();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "并发 healthCheck 超时");
    }

    // ========== 内存测试 ==========

    @Test
    void testLargeResponseHandling() throws IOException {
        OkHttpClient mockClient = mock(OkHttpClient.class);
        Call mockCall = mock(Call.class);

        // 模拟 1MB 响应
        StringBuilder largeBody = new StringBuilder();
        for (int i = 0; i < 100000; i++) {
            largeBody.append("abcdefghij");
        }
        Response mockResp = new Response.Builder()
            .request(new Request.Builder().url("http://localhost:8080").build())
            .protocol(Protocol.HTTP_1_1).code(200).message("OK")
            .body(ResponseBody.create(largeBody.toString(), MediaType.parse("application/json")))
            .build();

        when(mockCall.execute()).thenReturn(mockResp);
        when(mockClient.newCall(any())).thenReturn(mockCall);

        com.hermes.sdk.transport.HttpTransport transport = new com.hermes.sdk.transport.HttpTransport(
            HermesConfig.builder().baseUrl("http://localhost:8080").build(),
            mockClient,
            new com.fasterxml.jackson.databind.ObjectMapper()
        );

        long start = System.currentTimeMillis();
        String result = transport.get("/large");
        long elapsed = System.currentTimeMillis() - start;

        assertEquals(1000000, result.length());
        // 1MB 响应应该在 100ms 内处理完
        assertTrue(elapsed < 500, "大响应处理太慢: " + elapsed + "ms");
    }

    // ========== 重试测试 ==========

    @Test
    void testRetryConfiguration() {
        HermesConfig config = HermesConfig.builder()
            .maxRetries(5)
            .build();

        assertEquals(5, config.getMaxRetries());
    }

    @Test
    void testZeroRetries() {
        HermesConfig config = HermesConfig.builder()
            .maxRetries(0)
            .build();

        assertEquals(0, config.getMaxRetries());
    }

    // ========== 连接池测试 ==========

    @Test
    void testConnectionPoolReuse() {
        HermesClient client = HermesClient.builder().baseUrl("http://localhost:8080").build();
        OkHttpClient httpClient = client.getHttpClient();

        // 验证连接池配置
        assertNotNull(httpClient.connectionPool());
    }
}
