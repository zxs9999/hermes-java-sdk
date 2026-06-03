package com.hermes.sdk.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hermes.sdk.config.HermesConfig;
import com.hermes.sdk.dto.Session;
import com.hermes.sdk.exception.HermesApiException;
import com.hermes.sdk.exception.HermesNetworkException;
import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SessionService 单元测试（mock HTTP）
 */
class SessionServiceTest {

    private HermesConfig config;
    private OkHttpClient mockHttpClient;
    private ObjectMapper mapper;
    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        config = HermesConfig.builder().baseUrl("http://localhost:8080").build();
        mockHttpClient = mock(OkHttpClient.class);
        mapper = new ObjectMapper();
        sessionService = new SessionService(config, mockHttpClient, mapper);
    }

    // ========== list() ==========

    @Test
    void testListSuccess() throws IOException {
        String json = "[{\"sessionId\":\"s1\"},{\"sessionId\":\"s2\"}]";
        whenCall(200, json);

        List<Session> sessions = sessionService.list(20, 0);

        assertEquals(2, sessions.size());
        assertEquals("s1", sessions.get(0).getSessionId());
    }

    @Test
    void testListWithPagination() throws IOException {
        String json = "[{\"sessionId\":\"s1\"}]";
        whenCall(200, json);

        sessionService.list(10, 5);

        ArgumentCaptor<Request> reqCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mockHttpClient).newCall(reqCaptor.capture());
        String url = reqCaptor.getValue().url().toString();
        assertTrue(url.contains("limit=10"));
        assertTrue(url.contains("offset=5"));
    }

    @Test
    void testListHttpError() throws IOException {
        whenCall(404, "Not Found");

        assertThrows(HermesApiException.class, () -> sessionService.list(20, 0));
    }

    // ========== get() ==========

    @Test
    void testGetSuccess() throws IOException {
        whenCall(200, "{\"sessionId\":\"s1\",\"name\":\"my-session\"}");

        Session session = sessionService.get("s1");

        assertEquals("s1", session.getSessionId());
    }

    @Test
    void testGet404() throws IOException {
        whenCall(404, "not found");

        HermesApiException ex = assertThrows(HermesApiException.class, () -> sessionService.get("not-exist"));
        assertEquals(404, ex.getHttpStatus());
    }

    // ========== create() ==========

    @Test
    void testCreateSuccess() throws IOException {
        whenCall(200, "{\"sessionId\":\"new-sess\"}");

        Session session = sessionService.create();

        assertEquals("new-sess", session.getSessionId());
        verify(mockHttpClient).newCall(argThat((Request r) ->
            "POST".equals(r.method()) && r.url().encodedPath().contains("/api/sessions")));
    }

    @Test
    void testCreate500() throws IOException {
        whenCall(500, "Internal Error");

        HermesApiException ex = assertThrows(HermesApiException.class, () -> sessionService.create());
        assertEquals(500, ex.getHttpStatus());
    }

    // ========== delete() ==========

    @Test
    void testDeleteSuccess() throws IOException {
        whenCall(200, "");

        sessionService.delete("s1");

        verify(mockHttpClient).newCall(argThat((Request r) ->
            "DELETE".equals(r.method()) && r.url().encodedPath().contains("/api/sessions/s1")));
    }

    @Test
    void testDelete404() throws IOException {
        whenCall(404, "not found");

        assertThrows(HermesApiException.class, () -> sessionService.delete("not-exist"));
    }

    // ========== getMessages() ==========

    @Test
    void testGetMessagesSuccess() throws IOException {
        whenCall(200, "[{\"role\":\"user\",\"content\":\"hello\"},{\"role\":\"assistant\",\"content\":\"hi\"}]");

        var msgs = sessionService.getMessages("s1");

        assertEquals(2, msgs.size());
    }

    @Test
    void testGetMessages403() throws IOException {
        whenCall(403, "forbidden");

        assertThrows(HermesApiException.class, () -> sessionService.getMessages("s1"));
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