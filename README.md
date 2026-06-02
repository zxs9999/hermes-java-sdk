# Hermes Java SDK

Hermes Agent 的 Java SDK，支持 Spring Boot 3.x，提供工业级鲁棒性、线程安全、异步支持。

## 功能特性

| 特性 | 说明 |
|------|------|
| **异常细分** | HermesException → Network / Auth / Api 三类，可精准处理 |
| **重试机制** | 指数退避（1s/2s/4s），仅可恢复错误重试 |
| **线程安全** | `ThreadSafeChatSession` + `CopyOnWriteArrayList` |
| **异步支持** | `CompletableFuture` + `ExecutorService` |
| **日志框架** | Log4j2 2.24.1 + SLF4J 2.0.16，统一事件码 |
| **HTTPS 强制** | `requireHttps(true)` 默认开启 |
| **健康检查** | `hermesClient.healthCheck()` |
| **Builder 模式** | 链式配置，不可变对象 |

## 引入依赖

```xml
<dependency>
    <groupId>com.hermes</groupId>
    <artifactId>hermes-sdk-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 快速开始

### 1. 配置环境变量

```bash
export HERMES_API_KEY=sk-***
export HERMES_BASE_URL=https://api.hermes.com
```

### 2. application.yml（只需 base-url，其他用默认值）

```yaml
hermes:
  base-url: ${HERMES_BASE_URL:http://localhost:8080}
  # api-key 从 HERMES_API_KEY 环境变量读取

# 其他参数均为默认值，无需配置：
#   model: gpt-4
#   temperature: 0.7
#   max-tokens: 4096
#   connect-timeout: 30
#   read-timeout: 180
#   max-retries: 3
#   require-https: true
```

### 3. 使用 HermesClient

```java
// Builder 模式
HermesClient hermes = HermesClient.builder()
    .baseUrl(System.getenv("HERMES_BASE_URL"))
    .apiKey(System.getenv("HERMES_API_KEY"))
    .model("gpt-4")
    .temperature(0.7)
    .readTimeout(180)
    .maxRetries(3)
    .requireHttps(true)
    .build();

// 健康检查
if (!hermes.healthCheck()) {
    throw new IllegalStateException("Hermes 连接失败");
}

// 简单聊天
String result = hermes.chat("你好，Hermes！");

// 带 System Prompt
String result = hermes.chatWithSystemPrompt("你是一个代码审查助手", "审查这段代码...");
```

### 4. 异常处理

```java
try {
    String result = hermes.chat("分析代码");
} catch (HermesAuthException e) {
    // API Key 无效，不重试
    log.error("认证失败: {}", e.getMessage());
} catch (HermesNetworkException e) {
    // 网络问题，可重试
    log.warn("网络异常，第 {} 次重试", e.getMessage());
} catch (HermesApiException e) {
    // API 错误（429/5xx 可重试）
    log.error("API 错误: {} - {}", e.getErrorCode(), e.getMessage());
}
```

### 5. 激活 Skill

```java
String result = hermes.activateSkill("karpathy-principles", "重构 auth 模块");
```

### 6. 多轮对话

```java
// 线程安全版本（推荐）
ThreadSafeChatSession session = hermes.newThreadSafeSession();
session.chat("我想写小说");
session.chat("都市异能题材，300章");
String result = session.chat("开始写第1章");

// 非线程安全版本（单线程）
ChatSession session = hermes.newSession();
session.chat("问题1");
String answer = session.chat("问题2");
```

### 7. SkillService 便捷方法

```java
SkillService skillService = new SkillService(hermes);

// 同步调用
String design = skillService.rpgToJava(rpgSource);
String bid = skillService.generateBid(requirement);

// 异步调用
skillService.executeAsync("code-review", code)
    .thenAccept(review -> System.out.println("审查结果: " + review))
    .exceptionally(e -> {
        System.err.println("异步调用失败: " + e.getMessage());
        return null;
    });
```

## REST API

| 方法 | 接口 | 说明 |
|------|------|------|
| POST | `/api/hermes/chat` | 聊天 |
| POST | `/api/hermes/skill` | 激活 Skill |
| GET | `/api/hermes/health` | 健康检查 |

```bash
# 健康检查
curl http://localhost:8081/api/hermes/health

# 聊天
curl -X POST http://localhost:8081/api/hermes/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "你好"}'

# 激活 Skill
curl -X POST http://localhost:8081/api/hermes/skill \
  -H "Content-Type: application/json" \
  -d '{"skillName": "karpathy-principles", "task": "重构 auth"}'
```

## 日志格式

使用 `HermesLogger` + `LogEvents` 统一日志事件码：

```
14:32:01.123 [main] INFO  [HERMES_CHAT_REQUEST] >>> chat() 请求: 你好...
14:32:01.456 [main] INFO  [HERMES_CHAT_RESPONSE] <<< chat() 响应: 128 chars
14:32:02.789 [main] WARN  [HERMES_RETRY_NETWORK] attempt=1, delay=1000ms, msg=连接超时
14:32:04.123 [main] ERROR [HERMES_CHAT_ERROR] <<< chat() 失败: AUTH_ERROR, msg=API Key 无效
```

**LogEvents 事件码：**

| 事件码 | 说明 |
|--------|------|
| `HERMES_CHAT_REQUEST` | 聊天请求 |
| `HERMES_CHAT_RESPONSE` | 聊天响应 |
| `HERMES_CHAT_ERROR` | 聊天错误 |
| `HERMES_SKILL_ACTIVATE` | Skill 激活 |
| `HERMES_RETRY_NETWORK` | 网络重试 |
| `HERMES_RETRY_API` | API 重试 |
| `HERMES_HEALTH_CHECK` | 健康检查 |

## 文件结构

```
hermes-sdk/
├── pom.xml                      # 主 POM（Log4j2 2.24.1）
├── src/main/java/com/hermes/sdk/
│   ├── Demo.java                # 使用示例
│   ├── HermesApplication.java   # 启动类
│   ├── client/
│   │   ├── HermesClient.java       # 主客户端（重试/异常）
│   │   ├── ChatSession.java         # 多轮对话（非线程安全）
│   │   └── ThreadSafeChatSession.java  # 多轮对话（线程安全）
│   ├── config/
│   │   ├── HermesConfig.java       # 不可变配置（Builder）
│   │   └── HermesProperties.java    # Spring 配置属性
│   ├── controller/
│   │   └── HermesController.java    # REST API
│   ├── dto/
│   │   ├── ChatRequest.java         # REST 请求 DTO
│   │   └── SkillRequest.java        # Skill 请求 DTO
│   ├── exception/
│   │   ├── HermesException.java         # 基类
│   │   ├── HermesNetworkException.java  # 网络异常（可重试）
│   │   ├── HermesAuthException.java    # 认证异常（不重试）
│   │   └── HermesApiException.java      # API 异常（部分可重试）
│   ├── logging/
│   │   ├── HermesLogger.java       # 统一日志入口
│   │   └── LogEvents.java          # 日志事件码常量
│   ├── request/
│   │   └── OpenAIRequest.java      # OpenAI 兼容请求体
│   └── service/
│       └── SkillService.java       # Skill 服务 + 异步
├── src/main/resources/
│   ├── log4j2.xml                 # Log4j2 配置
│   └── application.yml            # Spring Boot 配置
└── src/test/java/
    ├── client/HermesClientTest.java
    └── exception/HermesExceptionTest.java
```

## 依赖

| 依赖 | 版本 | 说明 |
|------|------|------|
| Log4j2 | 2.24.1 | 日志框架 |
| SLF4J | 2.0.16 | 日志 API |
| OkHttp | 4.12.0 | HTTP 客户端 |
| Jackson | 2.18.x | JSON 处理 |
| Spring Boot | 3.x | 自动配置 |
| JUnit | 5.x | 单元测试 |

## 编译

```bash
mvn clean compile
```

## 运行测试

```bash
mvn test
```

## License

MIT · github.com/zxs9999/hermes-java-sdk