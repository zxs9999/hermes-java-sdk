package com.hermes.sdk.webhook;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 轻量级 HTTP Mock Server（基于 JDK 内置的 HttpServer）
 *
 * 用于测试 WebhookClient，避免引入 mockwebserver 依赖
 * （mockwebserver 在 aliyun maven 镜像中不可用）
 */
public class MockWebhookServer {

    private final HttpServer server;
    private final int port;
    private final List<MockResponse> responseQueue = new CopyOnWriteArrayList<>();
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private final AtomicReference<MockRequest> lastRequest = new AtomicReference<>();
    private volatile int defaultStatus = 202;
    private volatile String defaultBody = "{\"status\":\"accepted\",\"delivery_id\":\"wh-default\"}";

    public MockWebhookServer() {
        try {
            this.server = HttpServer.create(new InetSocketAddress(0), 0);
            this.port = server.getAddress().getPort();
            server.setExecutor(Executors.newCachedThreadPool());
            server.createContext("/", exchange -> {
                requestCount.incrementAndGet();
                byte[] body = exchange.getRequestBody().readAllBytes();
                MockRequest req = new MockRequest(
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().getPath(),
                    exchange.getRequestHeaders(),
                    new String(body));
                lastRequest.set(req);
                MockResponse resp = responseQueue.isEmpty()
                    ? new MockResponse(defaultStatus, defaultBody)
                    : responseQueue.remove(0);
                exchange.sendResponseHeaders(resp.status, resp.body.getBytes().length);
                exchange.getResponseBody().write(resp.body.getBytes());
                exchange.close();
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to start mock server", e);
        }
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    public int port() {
        return port;
    }

    public String url() {
        return "http://127.0.0.1:" + port;
    }

    public void enqueue(int status, String body) {
        responseQueue.add(new MockResponse(status, body));
    }

    public void enqueueOk() {
        enqueue(202, defaultBody);
    }

    public void enqueueUnauthorized() {
        enqueue(401, "{\"error\":\"invalid signature\"}");
    }

    public void enqueueInternalError() {
        enqueue(500, "{\"error\":\"internal error\"}");
    }

    public void setDefault(int status, String body) {
        this.defaultStatus = status;
        this.defaultBody = body;
    }

    public int requestCount() {
        return requestCount.get();
    }

    public MockRequest lastRequest() {
        return lastRequest.get();
    }

    public void reset() {
        requestCount.set(0);
        lastRequest.set(null);
        responseQueue.clear();
    }

    public static class MockResponse {
        public final int status;
        public final String body;
        public MockResponse(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }

    public static class MockRequest {
        public final String method;
        public final String path;
        public final java.util.Map<String, java.util.List<String>> headers;
        public final String body;
        public MockRequest(String method, String path,
                           java.util.Map<String, java.util.List<String>> headers,
                           String body) {
            this.method = method;
            this.path = path;
            this.headers = headers;
            this.body = body;
        }
        public String header(String name) {
            List<String> values = headers.get(name);
            if (values == null || values.isEmpty()) return null;
            return values.get(0);
        }
    }
}
