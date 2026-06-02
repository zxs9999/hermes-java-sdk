package com.hermes.sdk.service;

import com.hermes.sdk.client.HermesClient;
import lombok.extern.slf4j.Slf4j;

/**
 * Skill 激活服务
 * 
 * 提供便捷的 Skill 调用封装
 */
@Slf4j
public class SkillService {
    
    private final HermesClient client;
    
    public SkillService(HermesClient client) {
        this.client = client;
    }
    
    /**
     * 执行 Skill
     * 
     * @param skillName Skill 名称
     * @param task 任务描述
     * @return 执行结果
     */
    public String execute(String skillName, String task) {
        log.info("激活 Skill: {}, 任务: {}", skillName, task);
        return client.activateSkill(skillName, task);
    }
    
    /**
     * 执行 RPG 转 Java
     */
    public String rpgToJava(String rpgSource) {
        return execute("rpg-design", 
            "读取以下 RPG 源码，生成设计文档：\n\n" + rpgSource);
    }
    
    /**
     * 生成标书
     */
    public String generateBid(String requirement) {
        return execute("tender-writing", 
            "根据以下需求生成投标书：\n\n" + requirement);
    }
    
    /**
     * 生成设计文档
     */
    public String generateDesign(String sourceCode) {
        return execute("rpg-design", 
            "基于以下源码生成设计文档：\n\n" + sourceCode);
    }
    
    /**
     * 写小说章节
     */
    public String writeNovelChapter(String outline) {
        return execute("novel-writing", 
            "根据以下大纲写小说章节：\n\n" + outline);
    }
    
    /**
     * 代码审查
     */
    public String reviewCode(String code) {
        return execute("code-review", 
            "审查以下代码：\n\n" + code);
    }
    
    /**
     * 翻译代码（Python → Java）
     */
    public String translatePythonToJava(String pythonCode) {
        return execute("python-to-java", 
            "将以下 Python 代码翻译为 Java：\n\n" + pythonCode);
    }
}