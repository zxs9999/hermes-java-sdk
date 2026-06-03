package com.hermes.sdk.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DTO Jackson 序列化/反序列化测试
 */
class DtoSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    // ========== Session ==========

    @Test
    void testSessionRoundTrip() throws Exception {
        Session session = new Session();
        session.setSessionId("s1");
        session.setTitle("test-session");

        String json = mapper.writeValueAsString(session);
        Session parsed = mapper.readValue(json, Session.class);

        assertEquals("s1", parsed.getSessionId());
        assertEquals("test-session", parsed.getTitle());
    }

    @Test
    void testSessionDeserialize() throws Exception {
        String json = "{\"sessionId\":\"s2\",\"title\":\"my-session\"}";
        Session session = mapper.readValue(json, Session.class);

        assertEquals("s2", session.getSessionId());
        assertEquals("my-session", session.getTitle());
    }

    // ========== Run ==========

    @Test
    void testRunRoundTrip() throws Exception {
        Run run = new Run();
        run.setRunId("r1");
        run.setStatus("completed");

        String json = mapper.writeValueAsString(run);
        Run parsed = mapper.readValue(json, Run.class);

        assertEquals("r1", parsed.getRunId());
        assertEquals("completed", parsed.getStatus());
    }

    @Test
    void testRunDeserialize() throws Exception {
        String json = "{\"runId\":\"r2\",\"status\":\"pending\"}";
        Run run = mapper.readValue(json, Run.class);

        assertEquals("r2", run.getRunId());
        assertEquals("pending", run.getStatus());
    }

    // ========== Message ==========

    @Test
    void testMessageRoundTrip() throws Exception {
        Message msg = new Message();
        msg.setRole("user");
        msg.setContent("hello");

        String json = mapper.writeValueAsString(msg);
        Message parsed = mapper.readValue(json, Message.class);

        assertEquals("user", parsed.getRole());
        assertEquals("hello", parsed.getContent());
    }

    // ========== Skill ==========

    @Test
    void testSkillRoundTrip() throws Exception {
        Skill skill = new Skill();
        skill.setName("rpg-design");
        skill.setDescription("RPG转Java设计");

        String json = mapper.writeValueAsString(skill);
        Skill parsed = mapper.readValue(json, Skill.class);

        assertEquals("rpg-design", parsed.getName());
        assertEquals("RPG转Java设计", parsed.getDescription());
    }

    // ========== Toolset ==========

    @Test
    void testToolsetRoundTrip() throws Exception {
        Toolset toolset = new Toolset();
        toolset.setName("web-search");
        toolset.setEnabled(true);

        String json = mapper.writeValueAsString(toolset);
        Toolset parsed = mapper.readValue(json, Toolset.class);

        assertEquals("web-search", parsed.getName());
        assertTrue(parsed.isEnabled());
    }
}
