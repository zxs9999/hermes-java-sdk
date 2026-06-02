package com.hermes.sdk;

import com.hermes.sdk.client.HermesClient;
import com.hermes.sdk.client.ChatSession;
import com.hermes.sdk.service.SkillService;
import com.hermes.sdk.config.HermesConfig;

/**
 * Hermes SDK 使用示例
 */
public class Demo {
    
    public static void main(String[] args) {
        // 1. 创建客户端
        HermesClient hermes = new HermesClient(
            "http://localhost:8080",  // Hermes Gateway 地址
            "your-api-key"             // API Key
        );
        
        // 2. 简单聊天
        System.out.println("=== 简单聊天 ===");
        String result = hermes.chat("你好，Hermes！");
        System.out.println(result);
        
        // 3. 激活 Skill
        System.out.println("\n=== 激活 Skill ===");
        result = hermes.activateSkill("kimi-model-setup", "配置 Kimi 模型");
        System.out.println(result);
        
        // 4. 多轮对话
        System.out.println("\n=== 多轮对话 ===");
        ChatSession session = hermes.newSession();
        session.chat("我想写小说");
        session.chat("都市异能题材，300章");
        String novel = session.chat("开始写第1章");
        System.out.println(novel);
        
        // 5. SkillService 便捷调用
        System.out.println("\n=== SkillService ===");
        SkillService skillService = new SkillService(hermes);
        String design = skillService.generateDesign("RPG 源码...");
        System.out.println(design);
        
        // 6. 自定义配置
        System.out.println("\n=== 自定义配置 ===");
        HermesConfig config = new HermesConfig("http://localhost:8080", "key");
        config.setModel("gpt-4");
        config.setTemperature(0.5);
        config.setReadTimeout(300);
        
        HermesClient hermes2 = new HermesClient(config);
        result = hermes2.chat("分析代码");
        System.out.println(result);
    }
}