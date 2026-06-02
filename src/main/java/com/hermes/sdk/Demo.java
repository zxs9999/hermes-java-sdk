package com.hermes.sdk;

import com.hermes.sdk.client.HermesClient;
import com.hermes.sdk.core.HermesApi;
import com.hermes.sdk.dto.*;
import com.hermes.sdk.sdk.HermesSdk;
import com.hermes.sdk.service.SkillService;

import java.util.List;
import java.util.Map;

/**
 * Hermes SDK 使用示例
 * 
 * 展示两层 API：
 * 1. HermesClient — 聊天 + Skill 激活（主要入口）
 * 2. HermesApi    — 原始 API，一对一映射 gateway/platforms/api_server.py
 * 3. HermesSdk    — 业务封装，提供更易用的接口
 */
public class Demo {
    
    public static void main(String[] args) throws Exception {
        // ========== 1. 创建 HermesClient ==========
        HermesClient hermes = HermesClient.builder()
            .baseUrl(System.getenv("HERMES_BASE_URL"))
            .apiKey(System.getenv("HERMES_API_KEY"))
            .model("gpt-4")
            .build();
        
        // ========== 2. 创建 HermesApi（原始 API）==========
        HermesApi api = new HermesApi(
            hermes.getConfig(),
            hermes.getHttpClient(),
            hermes.getObjectMapper()
        );
        
        // ========== 3. 创建 HermesSdk（业务封装）==========
        HermesSdk sdk = new HermesSdk(
            hermes.getConfig(),
            hermes.getHttpClient(),
            hermes.getObjectMapper()
        );
        
        // ========== HermesClient 示例 ==========
        System.out.println("\n=== HermesClient: 聊天 ===");
        System.out.println(hermes.chat("你好"));
        
        // ========== HermesApi 示例（原始 API）==========
        System.out.println("\n=== HermesApi: 原始 API ===");
        
        // Skills
        System.out.println("\n--- Skills ---");
        List<Skill> skills = api.listSkills();
        skills.forEach(s -> System.out.println("  " + s.getName() + ": " + s.getDescription()));
        
        // Toolsets
        System.out.println("\n--- Toolsets ---");
        List<Toolset> toolsets = api.listToolsets();
        toolsets.forEach(t -> System.out.println("  " + t.getName() + " (enabled=" + t.isEnabled() + ", tools=" + t.getTools().size() + ")"));
        
        // Sessions
        System.out.println("\n--- Sessions ---");
        List<Session> sessions = api.listSessions(10, 0);
        sessions.forEach(s -> System.out.println("  " + s.getSessionId() + ": " + s.getTitle()));
        
        // ========== HermesSdk 示例（业务封装）==========
        System.out.println("\n=== HermesSdk: 业务封装 ===");
        
        // 系统监控
        System.out.println("\n--- 系统健康 ---");
        System.out.println("  健康: " + sdk.system().isHealthy());
        
        // 会话管理
        System.out.println("\n--- 会话管理 ---");
        Session newSession = sdk.sessions().create("测试会话");
        System.out.println("  创建: " + newSession.getSessionId());
        sdk.sessions().delete(newSession.getSessionId());
        System.out.println("  删除: " + newSession.getSessionId());
        
        // Toolset 查询
        System.out.println("\n--- Toolset 查询 ---");
        List<Toolset> enabledToolsets = sdk.toolsets().listEnabled();
        enabledToolsets.forEach(t -> System.out.println("  [enabled] " + t.getName() + ": " + t.getTools()));
        
        // Skill 查找
        System.out.println("\n--- Skill 查找 ---");
        api.listSkills().forEach(s -> System.out.println("  " + s.getName()));
        
        // ========== SkillService 示例 ==========
        System.out.println("\n=== SkillService: Skill 执行 ===");
        SkillService skillService = new SkillService(hermes);
        // String result = skillService.generateDesign("RPG 源码...");
        // System.out.println(result);
    }
}