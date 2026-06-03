package com.hermes.sdk.service;

import com.hermes.sdk.client.HermesClient;
import com.hermes.sdk.exception.HermesApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SkillService 单元测试（mock HermesClient）
 */
class SkillServiceTest {

    private HermesClient mockClient;
    private SkillService skillService;

    @BeforeEach
    void setUp() {
        mockClient = mock(HermesClient.class);
        skillService = new SkillService(mockClient);
    }

    // ========== execute() ==========

    @Test
    void testExecuteSuccess() {
        when(mockClient.activateSkill("rpg-design", "task-content")).thenReturn("result");

        String result = skillService.execute("rpg-design", "task-content");

        assertEquals("result", result);
        verify(mockClient).activateSkill("rpg-design", "task-content");
    }

    @Test
    void testExecuteExceptionPropagated() {
        when(mockClient.activateSkill("rpg-design", "task"))
            .thenThrow(new HermesApiException("SKILL_ERROR", 500));

        assertThrows(HermesApiException.class, () -> skillService.execute("rpg-design", "task"));
    }

    // ========== executeAsync() ==========

    @Test
    void testExecuteAsyncSuccess() throws Exception {
        when(mockClient.activateSkill("rpg-design", "task")).thenReturn("async-result");

        CompletableFuture<String> future = skillService.executeAsync("rpg-design", "task");

        assertEquals("async-result", future.get());
    }

    @Test
    void testExecuteAsyncException() {
        when(mockClient.activateSkill("rpg-design", "task"))
            .thenThrow(new HermesApiException("SKILL_ERROR", 500));

        CompletableFuture<String> future = skillService.executeAsync("rpg-design", "task");

        ExecutionException ex = assertThrows(ExecutionException.class, future::get);
        assertTrue(ex.getCause() instanceof HermesApiException);
    }

    // ========== 便捷方法 ==========

    @Test
    void testRpgToJava() {
        when(mockClient.activateSkill(eq("rpg-design"), contains("RPG 源码"))).thenReturn("design-doc");

        String result = skillService.rpgToJava("RPG SOURCE CODE");

        assertEquals("design-doc", result);
    }

    @Test
    void testGenerateBid() {
        when(mockClient.activateSkill(eq("tender-writing"), contains("投标书"))).thenReturn("bid-doc");

        String result = skillService.generateBid("requirement");

        assertEquals("bid-doc", result);
    }

    @Test
    void testGenerateDesign() {
        when(mockClient.activateSkill(eq("rpg-design"), contains("设计文档"))).thenReturn("design");

        String result = skillService.generateDesign("source code");

        assertEquals("design", result);
    }

    @Test
    void testWriteNovelChapter() {
        when(mockClient.activateSkill(eq("novel-writing"), contains("小说章节"))).thenReturn("chapter");

        String result = skillService.writeNovelChapter("outline");

        assertEquals("chapter", result);
    }

    @Test
    void testReviewCode() {
        when(mockClient.activateSkill(eq("code-review"), contains("审查"))).thenReturn("review");

        String result = skillService.reviewCode("code");

        assertEquals("review", result);
    }

    @Test
    void testTranslatePythonToJava() {
        when(mockClient.activateSkill(eq("python-to-java"), contains("Python"))).thenReturn("java-code");

        String result = skillService.translatePythonToJava("python code");

        assertEquals("java-code", result);
    }

    // ========== shutdown() ==========

    @Test
    void testShutdown() {
        SkillService svc = new SkillService(mockClient, Executors.newSingleThreadExecutor());
        svc.shutdown();
        // 不抛异常即通过
    }
}
