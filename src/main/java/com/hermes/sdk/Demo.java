package com.hermes.sdk;

import com.hermes.sdk.client.HermesClient;
import com.hermes.sdk.client.ThreadSafeChatSession;
import com.hermes.sdk.dto.Session;
import com.hermes.sdk.dto.Skill;
import com.hermes.sdk.dto.Toolset;
import com.hermes.sdk.service.*;

import java.util.List;
import java.util.Map;

/**
 * Hermes SDK 使用示例
 */
public class Demo {
    
    public static void main(String[] args) {
        // 1. 创建客户端（Builder 模式）
        // API Key 从环境变量 HERMES_API_KEY 读取
        HermesClient hermes = HermesClient.builder()
            .baseUrl(System.getenv("HERMES_BASE_URL"))
            .apiKey(System.getenv("HERMES_API_KEY"))
            .model("gpt-4")
            .temperature(0.7)
            .readTimeout(180)
            .maxRetries(3)
            .requireHttps(true)
            .build();
        
        // 2. 健康检查
        if (hermes.healthCheck()) {
            System.out.println("Hermes 连接正常");
        } else {
            System.out.println("Hermes 连接失败");
            return;
        }
        
        // 3. 简单聊天
        System.out.println("\n=== 简单聊天 ===");
        String result = hermes.chat("你好，Hermes！");
        System.out.println(result);
        
        // 4. 激活 Skill
        System.out.println("\n=== 激活 Skill ===");
        result = hermes.activateSkill("karpathy-principles", "重构 auth 模块");
        System.out.println(result);
        
        // 5. 多轮对话（线程安全）
        System.out.println("\n=== 多轮对话 ===");
        ThreadSafeChatSession session = hermes.newThreadSafeSession();
        session.chat("我想写小说");
        session.chat("都市异能题材，300章");
        String novel = session.chat("开始写第1章");
        System.out.println(novel);
        
        // 6. SkillService 便捷调用
        System.out.println("\n=== SkillService ===");
        SkillService skillService = new SkillService(hermes);
        String design = skillService.generateDesign("RPG 源码...");
        System.out.println(design);
        
        // 7. 异步调用
        System.out.println("\n=== 异步调用 ===");
        skillService.executeAsync("code-review", "public class Foo { }")
            .thenAccept(review -> System.out.println("审查结果: " + review))
            .exceptionally(e -> {
                System.err.println("异步调用失败: " + e.getMessage());
                return null;
            });
        
        // ========== ApiServerService ==========
        System.out.println("\n=== ApiServerService ===");
        ApiServerService apiServerService = new ApiServerService(
            hermes.getConfig(), hermes.getHttpClient(), hermes.getObjectMapper()
        );
        
        // 列出所有 Skills
        System.out.println("\n--- Skills 列表 ---");
        List<Skill> skills = apiServerService.listSkills();
        skills.forEach(s -> System.out.println("  " + s.getName() + ": " + s.getDescription()));
        
        // 列出所有 Toolsets
        System.out.println("\n--- Toolsets 列表 ---");
        List<Toolset> toolsets = apiServerService.listToolsets();
        toolsets.forEach(t -> System.out.println("  " + t.getName() + " (enabled=" + t.isEnabled() + ")"));
        
        // API 能力清单
        System.out.println("\n--- API Capabilities ---");
        Map<String, Object> caps = apiServerService.getCapabilities();
        System.out.println("  endpoints: " + caps.keySet());
        
        // 可用模型列表
        System.out.println("\n--- Models ---");
        Map<String, Object> models = apiServerService.listModels();
        System.out.println("  " + models);
        
        // 详细健康检查
        System.out.println("\n--- Health Detailed ---");
        Map<String, Object> health = apiServerService.healthDetailed();
        System.out.println("  " + health);
        
        // ========== SessionService ==========
        System.out.println("\n=== SessionService ===");
        SessionService sessionService = new SessionService(
            hermes.getConfig(), hermes.getHttpClient(), hermes.getObjectMapper()
        );
        
        // 列出所有会话
        System.out.println("\n--- Sessions 列表 ---");
        List<Session> sessions = sessionService.list(10, 0);
        sessions.forEach(s -> System.out.println("  " + s.getSessionId() + ": " + s.getTitle()));
        
        // 创建空白会话
        System.out.println("\n--- 创建会话 ---");
        Session newSession = sessionService.create();
        System.out.println("  created: " + newSession.getSessionId());
        
        // 在会话中聊天
        System.out.println("\n--- 会话聊天 ---");
        // sessionService.chat(newSession.getSessionId(), "你好");
        
        // 删除会话
        System.out.println("\n--- 删除会话 ---");
        sessionService.delete(newSession.getSessionId());
        System.out.println("  deleted: " + newSession.getSessionId());
        
        // ========== RunService ==========
        System.out.println("\n=== RunService ===");
        RunService runService = new RunService(
            hermes.getConfig(), hermes.getHttpClient(), hermes.getObjectMapper()
        );
        
        // 启动异步 Agent
        System.out.println("\n--- 启动异步 Agent ---");
        Map<String, Object> runResult = runService.startRun("帮我搜索 GitHub", null, null, null, null, false);
        String runId = (String) runResult.get("run_id");
        System.out.println("  run_id: " + runId);
        
        // 查询运行状态
        System.out.println("\n--- 查询运行状态 ---");
        Map<String, Object> status = runService.getRunStatus(runId);
        System.out.println("  status: " + status.get("status"));
        
        // 中断 Agent
        System.out.println("\n--- 中断 Agent ---");
        runService.stopRun(runId);
        System.out.println("  stopped: " + runId);
        
        // ========== 异常处理示例 ==========
        System.out.println("\n=== 异常处理 ===");
        try {
            hermes.chat("");
        } catch (IllegalArgumentException e) {
            System.out.println("输入校验失败: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("其他错误: " + e.getMessage());
        }
    }
}