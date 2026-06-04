package com.hermes.sdk.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hermes.sdk.exception.HermesApiException;
import com.hermes.sdk.exception.HermesException;
import com.hermes.sdk.exception.HermesNetworkException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Hermes Webhook 客户端
 *
 * 用于触发 Hermes Gateway 的 Webhook 路由。
 * Webhook 是 Hermes 暴露给外部系统的唯一 HTTP 入口（除消息平台外），
 * 适合事件驱动的异步任务（如：标书生成、状态变更通知）。
 *
 * <p>典型用法：</p>
 * <pre>{@code
 * WebhookConfig config = WebhookConfig.builder()
 *     .baseUrl("http://hermes-gateway:8644")
 *     .secret("bid-secret-2024")
 *     .build();
 * WebhookClient webhooks = new WebhookClient(config);
 *
 * WebhookResponse resp = webhooks.trigger("bid_draft", Map.of(
 *     "project_name", "BD2010",
 *     "project_type", "设计"
 * ));
 * // resp.getDeliveryId() 可用于追踪异步任务状态
 * }</pre>
 *
 * <p>前置条件：</p>
 * 需在 Hermes 的 ~/.hermes/config.yaml 中预配置 webhook route：
 * <pre>{@code
 * platforms:
 *   webhook:
 *     extra:
 *       routes:
 *         bid_draft:
 *           secret: "bid-secret-2024"
 *           prompt: "生成标书：{project_name}"
 *           skills: ["bid-generation"]
 *           deliver: "telegram"
 * }</pre>
 */
public class WebhookClient {

    private static final Logger log = LogManager.getLogger(WebhookClient.class);
    private static final String SDK_VERSION = "1.0.0";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final WebhookConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper mapper;

    public WebhookClient(WebhookConfig config) {
        this(config, null, null);
    }

    public WebhookClient(WebhookConfig config, OkHttpClient httpClient, ObjectMapper mapper) {
        if (config == null) {
            throw new IllegalArgumentException("WebhookConfig must not be null");
        }
        this.config = config;
        this.httpClient = httpClient != null ? httpClient : new OkHttpClient.Builder()
            .connectTimeout(config.getConnectTimeoutMs(), TimeUnit.MILLISECONDS)
            .readTimeout(config.getReadTimeoutMs(), TimeUnit.MILLISECONDS)
            .writeTimeout(config.getWriteTimeoutMs(), TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(false)
            .build();
        this.mapper = mapper != null ? mapper : new ObjectMapper();
    }

    /**
     * 触发 Webhook 事件
     *
     * @param routeName Webhook 路由名（需在 Hermes config.yaml 预配置）
     * @param payload   事件数据（序列化为 JSON）
     * @return Hermes 返回的响应
     * @throws HermesException 触发失败（4xx/5xx/网络错误）
     */
    public WebhookResponse trigger(String routeName, Map<String, Object> payload) {
        validateRouteName(routeName);
        validatePayload(payload);

        String url = config.getBaseUrl() + "/webhooks/" + routeName;
        String body;
        try {
            body = mapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new HermesApiException(
                "Failed to serialize payload: " + e.getMessage(), -1, e);
        }
        String signature = HMACSigner.sign(config.getSecret(), body);

        Request request = new Request.Builder()
            .url(url)
            .post(RequestBody.create(body, JSON))
            .addHeader("X-Webhook-Signature", signature)
            .addHeader("Content-Type", "application/json")
            .addHeader("User-Agent", "hermes-java-sdk/" + SDK_VERSION)
            .build();

        log.info("Triggering webhook: route={}, url={}, payloadKeys={}",
            routeName, url, payload.keySet());

        return executeWithRetry(request, 0);
    }

    private WebhookResponse executeWithRetry(Request request, int attempt) {
        try (Response response = httpClient.newCall(request).execute()) {
            int code = response.code();
            String responseBody = readBody(response);

            if (code >= 200 && code < 300) {
                log.info("Webhook triggered successfully: status={}, response={}", code, responseBody);
                return parseResponse(responseBody);
            }

            // 4xx 客户端错误，不重试
            if (code >= 400 && code < 500) {
                String msg = String.format("Webhook rejected: status=%d, body=%s", code, responseBody);
                log.warn(msg);
                throw new HermesApiException(msg, code);
            }

            // 5xx 服务器错误，可重试
            if (attempt < config.getMaxRetries()) {
                long backoff = config.getRetryBackoffMs() * (1L << attempt);
                log.warn("Webhook 5xx, retrying in {}ms (attempt {}/{}): status={}",
                    backoff, attempt + 1, config.getMaxRetries(), code);
                sleep(backoff);
                return executeWithRetry(request, attempt + 1);
            }

            String msg = String.format("Webhook failed after %d retries: status=%d, body=%s",
                config.getMaxRetries(), code, responseBody);
            throw new HermesApiException(msg, code);

        } catch (IOException e) {
            if (attempt < config.getMaxRetries()) {
                long backoff = config.getRetryBackoffMs() * (1L << attempt);
                log.warn("Webhook IO error, retrying in {}ms (attempt {}/{}): {}",
                    backoff, attempt + 1, config.getMaxRetries(), e.getMessage());
                sleep(backoff);
                return executeWithRetry(request, attempt + 1);
            }
            throw new HermesNetworkException(
                "Webhook IO error after " + config.getMaxRetries() + " retries: " + e.getMessage(), e);
        }
    }

    private WebhookResponse parseResponse(String body) {
        if (body == null || body.isEmpty()) {
            return new WebhookResponse("accepted", null, null);
        }
        try {
            return mapper.readValue(body, WebhookResponse.class);
        } catch (Exception e) {
            log.warn("Failed to parse webhook response, returning raw: {}", e.getMessage());
            return new WebhookResponse("accepted", null, body);
        }
    }

    private String readBody(Response response) {
        ResponseBody body = response.body();
        if (body == null) return "";
        try {
            return body.string();
        } catch (IOException e) {
            return "";
        }
    }

    private void sleep(long ms) {
        if (ms <= 0) return;
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HermesException("Webhook retry interrupted", e);
        }
    }

    private void validateRouteName(String routeName) {
        if (routeName == null || routeName.trim().isEmpty()) {
            throw new IllegalArgumentException("routeName must not be empty");
        }
    }

    private void validatePayload(Map<String, Object> payload) {
        if (payload == null) {
            throw new IllegalArgumentException("payload must not be null");
        }
    }
}
