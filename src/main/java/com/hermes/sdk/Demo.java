package com.hermes.sdk;

import com.hermes.sdk.client.HermesClient;
import com.hermes.sdk.client.ThreadSafeChatSession;
import com.hermes.sdk.service.SkillService;

/**
 * Hermes SDK 使用示例
 */
public class Demo {
    
    public static void main(String[] args) {
        // 1. 创建客户端（Builder 模式）
        HermesClient hermes = HermesClient.builder()
            .baseUrl("http://localhost:8080")
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
        
        // 8. 异常处理示例
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