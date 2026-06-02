# Hermes Java SDK

Hermes Agent Java SDK，支持 Spring Boot 3.x

## 功能

- OpenAI 兼容的 REST API 客户端
- Skill 激活封装
- 多轮对话会话
- Spring Boot 自动配置

## 引入依赖

```xml
<dependency>
    <groupId>com.hermes</groupId>
    <artifactId>hermes-sdk-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 配置

```yaml
# application.yml
hermes:
  base-url: http://localhost:8080
  api-key: your-api-key
  model: gpt-4
```

## 使用

### 1. 注入 HermesClient

```java
@RequiredArgsConstructor
public class MyService {
    
    private final HermesClient hermesClient;
    
    public void doSomething() {
        String result = hermesClient.chat("分析代码");
    }
}
```

### 2. 激活 Skill

```java
String result = hermesClient.activateSkill("kimi-model-setup", "配置 Kimi");
```

### 3. 多轮对话

```java
ChatSession session = hermesClient.newSession();
session.chat("写小说");
session.chat("都市异能");
String result = session.chat("开始写第1章");
```

### 4. SkillService

```java
@RequiredArgsConstructor
public class MyService {
    
    private final SkillService skillService;
    
    public void doSomething() {
        String result = skillService.rpgToJava(rpgSource);
    }
}
```

## API 接口

| 方法 | 接口 | 说明 |
|------|------|------|
| POST | `/api/hermes/chat` | 简单聊天 |
| POST | `/api/hermes/skill` | 激活 Skill |
| POST | `/api/hermes/chat/system` | 带 System Prompt |
| GET | `/api/hermes/health` | 健康检查 |

### 调用示例

```bash
# 聊天
curl -X POST http://localhost:8081/api/hermes/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "你好"}'

# 激活 Skill
curl -X POST http://localhost:8081/api/hermes/skill \
  -H "Content-Type: application/json" \
  -d '{"skillName": "kimi-model-setup", "task": "配置 Kimi"}'
```

## 文件结构

```
hermes-sdk/
├── pom-spring-boot.xml          # Spring Boot 版本
├── src/main/java/com/hermes/sdk/
│   ├── HermesApplication.java   # 启动类
│   ├── client/
│   │   ├── HermesClient.java   # 主客户端
│   │   └── ChatSession.java    # 多轮对话
│   ├── config/
│   │   └── HermesProperties.java  # 配置属性
│   ├── controller/
│   │   └── HermesController.java  # REST API
│   ├── dto/
│   │   ├── ChatRequest.java
│   │   └── SkillRequest.java
│   ├── service/
│   │   └── SkillService.java  # Skill 服务
│   └── autoconfig/
│       └── HermesAutoConfiguration.java  # 自动配置
└── src/main/resources/
    └── application.yml
```

## 编译

```bash
mvn -f pom-spring-boot.xml compile
```