package com.hermes.sdk.service;

import com.hermes.sdk.client.HermesClient;
import com.hermes.sdk.exception.HermesException;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * Skill 激活服务
 * 
 * 提供便捷的 Skill 调用封装 + 异步支持
 */
@Slf4j
public class SkillService {
    
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
        log.info("激活 Skill: {}, 任务: {}", skillName, maskTask(task));
        return client.activateSkill(skillName, task);
    }
    
    /**
     * 异步执行 Skill
     */
    public CompletableFuture<String> executeAsync(String skillName, String task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return execute(skillName, task);
            } catch (HermesException e) {
                throw new CompletionException(e);
            }
        }, executor);
    }
    
    /**
     * 执行 RPG 转 Java
     */
    public String rpgToJava(String rpgSource) {
        return execute("rpg-design", "读取以下 RPG 源码，生成设计文档：\n\n" + rpgSource);
    }
    
    /**
     * 异步执行 RPG 转 Java
     */
    public CompletableFuture<String> rpgToJavaAsync(String rpgSource) {
        return executeAsync("rpg-design", "读取以下 RPG 源码，生成设计文档：\n\n" + rpgSource);
    }
    
    /**
     * 生成标书
     */
    public String generateBid(String requirement) {
        return execute("tender-writing", "根据以下需求生成投标书：\n\n" + requirement);
    }
    
    /**
     * 异步生成标书
     */
    public CompletableFuture<String> generateBidAsync(String requirement) {
        return executeAsync("tender-writing", "根据以下需求生成投标书：\n\n" + requirement);
    }
    
    /**
     * 生成设计文档
     */
    public String generateDesign(String sourceCode) {
        return execute("rpg-design", "基于以下源码生成设计文档：\n\n" + sourceCode);
    }
    
    /**
     * 写小说章节
     */
    public String writeNovelChapter(String outline) {
        return execute("novel-writing", "根据以下大纲写小说章节：\n\n" + outline);
    }
    
    /**
     * 代码审查
     */
    public String reviewCode(String code) {
        return execute("code-review", "审查以下代码：\n\n" + code);
    }
    
    /**
     * 翻译代码（Python → Java）
     */
    public String translatePythonToJava(String pythonCode) {
        return execute("python-to-java", "将以下 Python 代码翻译为 Java：\n\n" + pythonCode);
    }
    
    /**
     * 任务脱敏（太长则截断）
     */
    private String maskTask(String task) {
        if (task == null) return "null";
        return task.length() > 50 ? task.substring(0, 50) + "..." : task;
    }
    
    /**
     * 关闭线程池
     */
    public void shutdown() {
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