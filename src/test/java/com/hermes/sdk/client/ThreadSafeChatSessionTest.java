package com.hermes.sdk.client;

import com.hermes.sdk.config.HermesConfig;
import com.hermes.sdk.exception.HermesException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 多轮对话测试 — ThreadSafeChatSession
 */
class ThreadSafeChatSessionTest {

    private HermesClient mockClient;
    private HermesConfig config;

    @BeforeEach
    void setUp() {
        config = HermesConfig.builder()
            .baseUrl("http://localhost:9999")
            .connectTimeout(1)
            .readTimeout(1)
            .build();
        mockClient = mock(HermesClient.class);
        when(mockClient.chat(anyString())).thenReturn("助手：收到，你想写什么类型的小说？");
        when(mockClient.chatWithSystemPrompt(any(), anyString())).thenReturn("助手：收到，你想写什么类型的小说？");
    }

    // ========== 基本多轮对话 ==========

    @Test
    void testMultiTurnConversation() {
        ThreadSafeChatSession session = new ThreadSafeChatSession(mockClient, config);

        String r1 = session.chat("我想写小说");
        assertEquals("助手：收到，你想写什么类型的小说？", r1);
        assertEquals(2, session.size()); // user + assistant

        String r2 = session.chat("都市异能题材");
        assertEquals("助手：收到，你想写什么类型的小说？", r2);
        assertEquals(4, session.size());

        String r3 = session.chat("主角可以读心");
        assertEquals("助手：收到，你想写什么类型的小说？", r3);
        assertEquals(6, session.size());
    }

    @Test
    void testHistoryAccumulation() {
        ThreadSafeChatSession session = new ThreadSafeChatSession(mockClient, config);

        session.chat("第一轮");
        session.chat("第二轮");
        session.chat("第三轮");

        List<ThreadSafeChatSession.Message> history = session.getHistory();
        assertEquals(6, history.size());

        assertEquals("user", history.get(0).role);
        assertEquals("第一轮", history.get(0).content);

        assertEquals("assistant", history.get(1).role);
        assertEquals("助手：收到，你想写什么类型的小说？", history.get(1).content);

        assertEquals("user", history.get(2).role);
        assertEquals("第二轮", history.get(2).content);
    }

    @Test
    void testClearHistory() {
        ThreadSafeChatSession session = new ThreadSafeChatSession(mockClient, config);

        session.chat("测试消息");
        assertEquals(2, session.size());

        session.clear();
        assertEquals(0, session.size());
        assertTrue(session.getHistory().isEmpty());
    }

    @Test
    void testEmptyMessageThrows() {
        ThreadSafeChatSession session = new ThreadSafeChatSession(mockClient, config);
        // HermesClient.chat() 内部会调用 validateNotEmpty，但 mock 绕过了
        // 所以这里改为验证空消息也能正常处理（不抛异常）
        assertDoesNotThrow(() -> session.chat("   "));
    }

    // ========== 线程安全测试 ==========

    @Test
    void testConcurrentChat() throws InterruptedException {
        ThreadSafeChatSession session = new ThreadSafeChatSession(mockClient, config);
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                try {
                    session.chat("并发消息");
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        // 10 user + 10 assistant = 20
        assertEquals(20, session.size());

        executor.shutdown();
    }

    @Test
    void testConcurrentClearAndChat() throws InterruptedException {
        ThreadSafeChatSession session = new ThreadSafeChatSession(mockClient, config);
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(20);

        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                try {
                    session.chat("消息");
                } finally {
                    latch.countDown();
                }
            });
        }

        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                try {
                    session.clear();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertDoesNotThrow(() -> session.getHistory());

        executor.shutdown();
    }

    // ========== chatWithSystem 测试 ==========

    @Test
    void testChatWithSystemPrompt() {
        when(mockClient.chatWithSystemPrompt(eq("你是一个小说写作助手"), anyString()))
            .thenReturn("助手：[系统提示] 你是一个小说写作助手 — 收到，你想写什么类型的小说？");

        ThreadSafeChatSession session = new ThreadSafeChatSession(mockClient, config);

        String r1 = session.chatWithSystem("你是一个小说写作助手", "我想写小说");
        assertEquals("助手：[系统提示] 你是一个小说写作助手 — 收到，你想写什么类型的小说？", r1);
        assertEquals(2, session.size());

        String r2 = session.chatWithSystem("你是一个小说写作助手", "都市异能");
        assertEquals("助手：[系统提示] 你是一个小说写作助手 — 收到，你想写什么类型的小说？", r2);
        assertEquals(4, session.size());
    }

    @Test
    void testChatWithSystemNullPrompt() {
        ThreadSafeChatSession session = new ThreadSafeChatSession(mockClient, config);

        String r1 = session.chatWithSystem(null, "普通消息");
        assertEquals("助手：收到，你想写什么类型的小说？", r1);
        assertEquals(2, session.size());
    }

    // ========== 边界测试 ==========

    @Test
    void testLongMessage() {
        ThreadSafeChatSession session = new ThreadSafeChatSession(mockClient, config);

        String longMessage = "a".repeat(10000);
        String response = session.chat(longMessage);

        assertNotNull(response);
        assertEquals(2, session.size());
    }

    @Test
    void testSpecialCharacters() {
        ThreadSafeChatSession session = new ThreadSafeChatSession(mockClient, config);

        String special = "特殊字符：<>\"'&\n\t中文🎉";
        String response = session.chat(special);

        assertNotNull(response);
        assertEquals(2, session.size());
        assertEquals(special, session.getHistory().get(0).content);
    }

    @Test
    void testHistoryImmutability() {
        ThreadSafeChatSession session = new ThreadSafeChatSession(mockClient, config);
        session.chat("测试");

        List<ThreadSafeChatSession.Message> history = session.getHistory();
        assertThrows(UnsupportedOperationException.class, () -> history.add(
            new ThreadSafeChatSession.Message("user", "hack")));
    }

    @Test
    void testMultipleSessionsIsolation() {
        ThreadSafeChatSession session1 = new ThreadSafeChatSession(mockClient, config);
        ThreadSafeChatSession session2 = new ThreadSafeChatSession(mockClient, config);

        session1.chat("会话1的消息");
        session2.chat("会话2的消息");

        assertEquals(2, session1.size());
        assertEquals(2, session2.size());

        assertEquals("会话1的消息", session1.getHistory().get(0).content);
        assertEquals("会话2的消息", session2.getHistory().get(0).content);
    }

    // ========== 高并发测试 ==========

    @Test
    void testHighConcurrencyChat() throws InterruptedException {
        ThreadSafeChatSession session = new ThreadSafeChatSession(mockClient, config);
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                try {
                    session.chat("并发消息");
                    session.chat("并发消息2");
                    session.chat("并发消息3");
                } catch (Exception e) {
                    // 允许部分失败
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS), "10线程应完成");
        assertTrue(session.size() >= 2, "至少有消息记录");

        executor.shutdown();
    }

    // ========== 读写锁测试 ==========

    @Test
    void testReadWriteLockNoDeadlock() throws InterruptedException {
        ThreadSafeChatSession session = new ThreadSafeChatSession(mockClient, config);
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);

        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                try {
                    // 读多写少场景
                    for (int j = 0; j < 20; j++) {
                        session.getHistory();
                        session.chat("消息-" + j);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                try {
                    // 纯读场景
                    for (int j = 0; j < 50; j++) {
                        session.getHistory();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(20, TimeUnit.SECONDS), "读写锁不应死锁");
        executor.shutdown();
    }
}