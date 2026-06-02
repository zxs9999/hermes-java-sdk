package com.hermes.sdk.service;

import com.hermes.sdk.client.HermesClient;
import com.hermes.sdk.exception.HermesException;
import com.hermes.sdk.logging.HermesLogger;
import com.hermes.sdk.logging.LogEvents;

import java.util.concurrent.*;
import org.slf4j.Logger;

/**
 * Skill 激活服务
 * 
 * 提供便捷的 Skill 调用封装 + 异步支持
 */
public class SkillService {
    
    private static final Logger log = HermesLogger.get(SkillService.class);
    
    private final HermesClient client;
    private final ExecutorService executor;
    
    public SkillService(HermesClient client) {
        this(client, Executors.newCachedThreadPool());
    }
    
    public SkillService(HermesClient client, ExecutorService executor) {
        this.client = client;
        this.executor = executor;
    }
    
    /**
     * 执行 Skill
     */
    public String execute(String skillName, String task) {
        log.info("[{}] skillName={}, task={}", 
            LogEvents.SKILL_ACTIVATE, skillName, maskContent(task));
        try {
            String result = client.activateSkill(skillName, task);
            log.info("[{}] skillName={}, resultSize={} chars", 
                LogEvents.SKILL_ACTIVATE, skillName, result.length());
            return result;
        } catch (HermesException e) {
            log.error("[{}] skillName={}, failed: errorCode={}, msg={}", 
                LogEvents.SKILL_ACTIVATE, skillName, e.getErrorCode(), e.getMessage());
            throw e;
        }
    }
    
    /**
     * 异步执行 Skill
     */
    public CompletableFuture<String> executeAsync(String skillName, String task) {
        log.info("[{}] skillName={}, task={}, async=true", 
            LogEvents.SKILL_ASYNC_START, skillName, maskContent(task));
        return CompletableFuture.supplyAsync(() -> {
            try {
                String result = execute(skillName, task);
                log.info("[{}] skillName={}, done, resultSize={} chars", 
                    LogEvents.SKILL_ASYNC_DONE, skillName, result.length());
                return result;
            } catch (HermesException e) {
                log.error("[{}] skillName={}, async failed: errorCode={}, msg={}", 
                    LogEvents.SKILL_ASYNC_ERROR, skillName, e.getErrorCode(), e.getMessage());
                throw new CompletionException(e);
            }
        }, executor);
    }
    
    /**
     * 执行 RPG 转 Java
     */
    public String rpgToJava(String rpgSource) {
        log.info("[{}] rpgToJava, sourceLength={}", 
            LogEvents.SKILL_ACTIVATE, rpgSource.length());
        return execute("rpg-design", "读取以下 RPG 源码，生成设计文档：\n\n" + rpgSource);
    }
    
    /**
     * 异步执行 RPG 转 Java
     */
    public CompletableFuture<String> rpgToJavaAsync(String rpgSource) {
        log.info("[{}] rpgToJavaAsync, sourceLength={}, async=true", 
            LogEvents.SKILL_ASYNC_START, rpgSource.length());
        return executeAsync("rpg-design", "读取以下 RPG 源码，生成设计文档：\n\n" + rpgSource);
    }
    
    /**
     * 生成标书
     */
    public String generateBid(String requirement) {
        log.info("[{}] generateBid, requirementLength={}", 
            LogEvents.SKILL_ACTIVATE, requirement.length());
        return execute("tender-writing", "根据以下需求生成投标书：\n\n" + requirement);
    }
    
    /**
     * 异步生成标书
     */
    public CompletableFuture<String> generateBidAsync(String requirement) {
        log.info("[{}] generateBidAsync, requirementLength={}, async=true", 
            LogEvents.SKILL_ASYNC_START, requirement.length());
        return executeAsync("tender-writing", "根据以下需求生成投标书：\n\n" + requirement);
    }
    
    /**
     * 生成设计文档
     */
    public String generateDesign(String sourceCode) {
        log.info("[{}] generateDesign, sourceLength={}", 
            LogEvents.SKILL_ACTIVATE, sourceCode.length());
        return execute("rpg-design", "基于以下源码生成设计文档：\n\n" + sourceCode);
    }
    
    /**
     * 写小说章节
     */
    public String writeNovelChapter(String outline) {
        log.info("[{}] writeNovelChapter, outlineLength={}", 
            LogEvents.SKILL_ACTIVATE, outline.length());
        return execute("novel-writing", "根据以下大纲写小说章节：\n\n" + outline);
    }
    
    /**
     * 代码审查
     */
    public String reviewCode(String code) {
        log.info("[{}] reviewCode, codeLength={}", 
            LogEvents.SKILL_ACTIVATE, code.length());
        return execute("code-review", "审查以下代码：\n\n" + code);
    }
    
    /**
     * 翻译代码（Python → Java）
     */
    public String translatePythonToJava(String pythonCode) {
        log.info("[{}] translatePythonToJava, codeLength={}", 
            LogEvents.SKILL_ACTIVATE, pythonCode.length());
        return execute("python-to-java", "将以下 Python 代码翻译为 Java：\n\n" + pythonCode);
    }
    
    /**
     * 内容脱敏（太长则截断）
     */
    private String maskContent(String content) {
        if (content == null) return "null";
        return content.length() > 50 
            ? content.substring(0, 50) + "...(length=" + content.length() + ")" 
            : content;
    }
    
    /**
     * 关闭线程池
     */
    public void shutdown() {
        log.info("[{}] shutdown executor", LogEvents.SESSION_CLEAR);
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}