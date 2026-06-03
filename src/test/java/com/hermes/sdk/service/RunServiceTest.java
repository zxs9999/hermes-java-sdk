package com.hermes.sdk.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hermes.sdk.config.HermesConfig;
import com.hermes.sdk.exception.HermesApiException;
import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RunService 单元测试（mock HTTP）
 */
class RunServiceTest {

    private HermesConfig config;
    private OkHttpClient mockHttpClient;
    private ObjectMapper mapper;
    private RunService runService;

    @BeforeEach
    void setUp() {
        config = HermesConfig.builder().baseUrl("http://localhost:8080").build();
        mockHttpClient = mock(OkHttpClient.class);
        mapper = new ObjectMapper();
        runService = new RunService(config, mockHttpClient, mapper);
    }

    // ========== startRun() ==========

    @Test
    void testStartRunSuccess() throws IOException {
        whenCall(200, "{\"run_id\":\"r1\",\"status\":\"pending\"}");

        Map<String, Object> ctx = runService.startRun("hello");

        assertNotNull(ctx);
        assertEquals("r1", ctx.get("run_id"));
        verify(mockHttpClient).newCall(argThat((Request r) ->
            "POST".equals(r.method()) && r.url().encodedPath().contains("/v1/runs")));
    }

    @Test
    void testStartRunWithOptions() throws IOException {
        whenCall(200, "{\"run_id\":\"r2\",\"status\":\"pending\"}");

        Map<String, Object> ctx = runService.startRun("task", "gpt-4", "agent", "s1", "you are helpful", false);

        assertNotNull(ctx);
    }

    @Test
    void testStartRun500() throws IOException {
        whenCall(500, "server error");

        HermesApiException ex = assertThrows(HermesApiException.class, () ->
            runService.startRun("msg"));
        assertEquals(500, ex.getHttpStatus());
    }

    // ========== getRunStatus() ==========

    @Test
    void testGetRunStatusSuccess() throws IOException {
        whenCall(200, "{\"run_id\":\"r1\",\"status\":\"completed\"}");

        Map<String, Object> status = runService.getRunStatus("r1");

        assertEquals("r1", status.get("run_id"));
        assertEquals("completed", status.get("status"));
    }

    @Test
    void testGetRunStatus404() throws IOException {
        whenCall(404, "not found");

        assertThrows(HermesApiException.class, () -> runService.getRunStatus("not-exist"));
    }

    // ========== approveRun() ==========

    @Test
    void testApproveRunApprove() throws IOException {
        whenCall(200, "{\"run_id\":\"r1\",\"status\":\"completed\"}");

        Map<String, Object> ctx = runService.approveRun("r1", "approve");

        assertNotNull(ctx);
        verify(mockHttpClient).newCall(argThat((Request r) ->
            "POST".equals(r.method()) && r.url().encodedPath().contains("/approval")));
    }

    @Test
    void testApproveRunReject() throws IOException {
        whenCall(200, "{\"run_id\":\"r1\",\"status\":\"cancelled\"}");

        Map<String, Object> ctx = runService.approveRun("r1", "reject");

        assertNotNull(ctx);
    }

    @Test
    void testApproveRun403() throws IOException {
        whenCall(403, "forbidden");

        assertThrows(HermesApiException.class, () ->
            runService.approveRun("r1", "approve"));
    }

    // ========== Helper ==========

    private void whenCall(int status, String body) throws IOException {
        Response mockResp = new Response.Builder()
            .request(new Request.Builder().url("http://localhost:8080").build())
            .protocol(Protocol.HTTP_1_1).code(status).message("OK")
            .body(ResponseBody.create(body, MediaType.parse("application/json")))
            .build();

        Call mockCall = mock(Call.class);
        when(mockCall.execute()).thenReturn(mockResp);
        when(mockHttpClient.newCall(any(Request.class))).thenReturn(mockCall);
    }
}