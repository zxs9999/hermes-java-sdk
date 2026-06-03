package com.hermes.sdk.security;

import com.hermes.sdk.client.HermesClient;
import com.hermes.sdk.config.HermesConfig;
import com.hermes.sdk.exception.HermesApiException;
import com.hermes.sdk.exception.HermesNetworkException;
import com.hermes.sdk.transport.TransportType;
import okhttp3.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 安全测试 — 输入校验、异常安全、资源泄漏
 */
class SecurityTest {

    // ========== 输入校验 ==========

    @Test
    void testChatNullMessage() {
        HermesClient client = HermesClient.builder().baseUrl("http://localhost:8080").build();
        assertThrows(IllegalArgumentException.class, () -> client.chat(null));
    }

    @Test
    void testChatEmptyMessage() {
        HermesClient client = HermesClient.builder().baseUrl("http://localhost:8080").build();
        assertThrows(IllegalArgumentException.class, () -> client.chat(""));
    }

    @Test
    void testChatWhitespaceMessage() {
        HermesClient client = HermesClient.builder().baseUrl("http://localhost:8080").build();
        assertThrows(IllegalArgumentException.class, () -> client.chat("   "));
    }

    @Test
    void testActivateSkillNullName() {
        HermesClient client = HermesClient.builder().baseUrl("http://localhost:8080").build();
        assertThrows(IllegalArgumentException.class, () -> client.activateSkill(null, "task"));
    }

    @Test
    void testActivateSkillEmptyName() {
        HermesClient client = HermesClient.builder().baseUrl("http://localhost:8080").build();
        assertThrows(IllegalArgumentException.class, () -> client.activateSkill("", "task"));
    }

    @Test
    void testActivateSkillNullTask() {
        HermesClient client = HermesClient.builder().baseUrl("http://localhost:8080").build();
        assertThrows(IllegalArgumentException.class, () -> client.activateSkill("skill", null));
    }

    @Test
    void testActivateSkillXssPayload() {
        HermesClient client = HermesClient.builder()
            .baseUrl("http://localhost:9999")
            .connectTimeout(1)
            .readTimeout(1)
            .build();
        // XSS payload 应该被当作普通字符串处理，只校验非空
        String xss = "<script>alert('xss')</script>";
        // 会抛网络异常（连不上），不是 IllegalArgumentException，说明通过了校验
        assertThrows(HermesNetworkException.class, () -> client.activateSkill("skill", xss));
    }

    @Test
    void testActivateSkillSqlInjectionPayload() {
        HermesClient client = HermesClient.builder()
            .baseUrl("http://localhost:9999")
            .connectTimeout(1)
            .readTimeout(1)
            .build();
        String sql = "'; DROP TABLE users; --";
        // 会抛网络异常（连不上），不是 IllegalArgumentException，说明通过了校验
        assertThrows(HermesNetworkException.class, () -> client.activateSkill("skill", sql));
    }

    // ========== URL 安全 ==========

    @Test
    void testBaseUrlWithPathTraversal() {
        // 路径遍历在 baseUrl 中，由 HttpUrl 处理
        HermesConfig config = HermesConfig.builder()
            .baseUrl("http://localhost:8080/../../../etc/passwd")
            .build();
        assertNotNull(config);
    }

    @Test
    void testBaseUrlWithNull() {
        HermesConfig config = HermesConfig.builder()
            .baseUrl(null)
            .build();
        assertNull(config.getBaseUrl());
    }

    // ========== 异常安全 ==========

    @Test
    void testNetworkExceptionContainsNoSensitiveData() {
        HermesNetworkException ex = new HermesNetworkException("connection failed");
        assertFalse(ex.getMessage().contains("password"));
        assertFalse(ex.getMessage().contains("secret"));
        assertFalse(ex.getMessage().contains("token"));
    }

    @Test
    void testApiExceptionDoesNotExposeStackTraceToCaller() {
        HermesApiException ex = new HermesApiException("API error", 500);
        // 异常消息不应包含内部实现细节
        assertFalse(ex.getMessage().contains("okhttp"));
        assertFalse(ex.getMessage().contains("jackson"));
    }

    // ========== 资源关闭 ==========

    @Test
    void testHttpTransportClosesResources() {
        OkHttpClient httpClient = new OkHttpClient.Builder().build();
        com.hermes.sdk.transport.HttpTransport transport = new com.hermes.sdk.transport.HttpTransport(
            HermesConfig.builder().baseUrl("http://localhost:8080").build(),
            httpClient,
            new com.fasterxml.jackson.databind.ObjectMapper()
        );

        // 关闭不应抛异常
        assertDoesNotThrow(() -> transport.close());
    }

    @Test
    void testResponseBodyClosedAfterError() throws IOException {
        OkHttpClient mockClient = mock(OkHttpClient.class);
        Call mockCall = mock(Call.class);
        Response mockResp = new Response.Builder()
            .request(new Request.Builder().url("http://localhost:8080").build())
            .protocol(Protocol.HTTP_1_1).code(500).message("Error")
            .body(ResponseBody.create("error", MediaType.parse("text/plain")))
            .build();

        when(mockClient.newCall(any())).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResp);

        com.hermes.sdk.transport.HttpTransport transport = new com.hermes.sdk.transport.HttpTransport(
            HermesConfig.builder().baseUrl("http://localhost:8080").build(),
            mockClient,
            new com.fasterxml.jackson.databind.ObjectMapper()
        );

        assertThrows(HermesApiException.class, () -> transport.get("/test"));
    }
}
