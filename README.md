# Hermes Java SDK

Hermes Agent 的 Java SDK，支持 Spring Boot 3.x，提供工业级鲁棒性、线程安全、异步支持。

## 分层架构

```
┌─────────────────────────────────────────────┐
│  HermesClient — 聊天 + Skill 激活（主要入口）│
├─────────────────────────────────────────────┤
│  HermesSdk   — 业务封装                      │
│    ├─ SessionManager   会话管理（含缓存）     │
│    ├─ ToolsetManager  Toolset 查询+缓存       │
│    ├─ SkillExecutor   Skill 查询             │
│    └─ SystemMonitor   系统监控               │
├─────────────────────────────────────────────┤
│  HermesApi   — 原始 API（一对一映射 gateway） │
│    ├─ Skills / Toolsets / Capabilities       │
│    ├─ Sessions CRUD + Messages               │
│    └─ Runs 异步启动/查询/审批/停止           │
├─────────────────────────────────────────────┤
│  ApiServerCore — HTTP 底层（GET/POST/PATCH/DELETE）
└─────────────────────────────────────────────┘
```

## 功能特性

| 特性 | 说明 |
|------|------|
| **分层设计** | Core（底层）→ Api（原始）→ Sdk（业务）三层分离 |
| **异常细分** | HermesException → Network / Auth / Api 三类，可精准处理 |
| **重试机制** | 指数退避（1s → 2s → 4s），最多 3 次 |
| **线程安全** | ThreadSafeChatSession / CopyOnWriteArrayList |
| **异步支持** | CompletableFuture + thenAccept |
| **日志框架** | Log4j2 2.24.1 + SLF4J，统一事件码 |
| **HTTPS 强制** | requireHttps(true) 防止明文泄露 |
| **健康检查** | 简单 + 详细两种模式 |
| **Builder 模式** | 不可变配置，编译期校验 |

## 环境变量

```bash
export HERMES_BASE_URL=http://localhost:8080
export HERMES_API_KEY=your-api-key
```

## 快速开始

### 1. Maven 依赖

```xml
<dependency>
    <groupId>com.hermes</groupId>
    <artifactId>hermes-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. application.yml

```yaml
hermes:
  base-url: ${HERMES_BASE_URL:http://localhost:8080}
  # api-key 从 HERMES_API_KEY 环境变量读取
```

### 3. 使用示例

```java
// 创建客户端
HermesClient hermes = HermesClient.builder()
    .baseUrl(System.getenv("HERMES_BASE_URL"))
    .apiKey(System.getenv("HERMES_API_KEY"))
    .build();

// 聊天
String result = hermes.chat("你好，Hermes！");

// 带 Skill 激活
String result = hermes.activateSkill("karpathy-principles", "重构 auth 模块");

// 线程安全多轮对话
ThreadSafeChatSession session = hermes.newThreadSafeSession();
session.chat("我想写小说");
session.chat("都市异能题材");
String novel = session.chat("开始写第1章");
```

## 三层 API 详解

### HermesClient（主要入口）

聊天和 Skill 激活：

```java
// 简单聊天
hermes.chat("分析这段代码");

// 带系统提示
hermes.chatWithSystemPrompt("你是一个代码审查员", "审查这段代码");

// 激活 Skill
hermes.activateSkill("karpathy-principles", "重构 auth 模块");

// 线程安全会话
ThreadSafeChatSession session = hermes.newThreadSafeSession();
session.chat("第一轮对话");
session.chat("第二轮对话");

// 健康检查
if (hermes.healthCheck()) {
    System.out.println("连接正常");
}
```

### HermesSdk（业务封装）

```java
HermesSdk sdk = new HermesSdk(config, httpClient, mapper);

// 系统监控
boolean healthy = sdk.system().isHealthy();
Map<String, Object> caps = sdk.system().getCapabilities();

// 会话管理
Session session = sdk.sessions().create("我的会话");
sdk.sessions().delete(session.getSessionId());
List<Session> recent = sdk.sessions().listRecent(10);

// Toolset 查询
List<Toolset> enabled = sdk.toolsets().listEnabled();
Optional<Toolset> toolset = sdk.toolsets().findByName("github");
List<String> tools = sdk.toolsets().getTools("github");
```

### HermesApi（原始 API）

一对一对应 `gateway/platforms/api_server.py`：

```java
HermesApi api = new HermesApi(config, httpClient, mapper);

// Skills & Toolsets
List<Skill> skills = api.listSkills();
List<Toolset> toolsets = api.listToolsets();

// Sessions
List<Session> sessions = api.listSessions(10, 0);
Session session = api.createSession(null, null, "标题", null);
api.deleteSession(sessionId);
List<Message> messages = api.getSessionMessages(sessionId);

// Runs
Map<String, Object> run = api.startRun("任务描述", null, null, null, null, false);
Map<String, Object> status = api.getRunStatus(runId);
api.stopRun(runId);

// Health
boolean healthy = api.health();
Map<String, Object> detailed = api.healthDetailed();
```

### ApiServerCore（HTTP 底层）

如果需要直接构造 HTTP 请求：

```java
ApiServerCore core = new ApiServerCore(config, httpClient, mapper);

// GET
String body = core.get("v1/skills");

// POST
String body = core.post("v1/runs", Map.of("message", "hello"));

// 带查询参数
String body = core.get("api/sessions", Map.of("limit", "10", "offset", "0"));
```

## 日志事件码

| 事件码 | 说明 |
|--------|------|
| `HERMES_CHAT_REQUEST` | 聊天请求 |
| `HERMES_CHAT_RESPONSE` | 聊天响应 |
| `HERMES_CHAT_ERROR` | 聊天错误 |
| `HERMES_SKILL_ACTIVATE` | Skill 激活 |
| `HERMES_RETRY_NETWORK` | 网络重试 |
| `HERMES_SESSION_*` | 会话操作 |
| `HERMES_RUN_*` | Run 操作 |
| `HERMES_SKILLS_LIST` | Skills 列表 |

## 完整文件结构

```
src/main/java/com/hermes/sdk/
├── Demo.java                    — 使用示例
├── client/
│   ├── HermesClient.java       — 主客户端
│   ├── ChatSession.java        — 多轮会话
│   └── ThreadSafeChatSession.java
├── config/
│   ├── HermesConfig.java       — Builder 配置
│   └── HermesProperties.java   — Spring 配置
├── controller/
│   └── HermesController.java   — REST API
├── core/
│   ├── ApiServerCore.java      — HTTP 底层
│   └── HermesApi.java          — 原始 API
├── dto/
│   ├── ChatRequest.java        — 聊天请求
│   ├── Message.java            — 消息
│   ├── Run.java                — Run
│   ├── Session.java            — 会话
│   ├── Skill.java              — Skill
│   ├── SkillRequest.java       — Skill 请求
│   └── Toolset.java            — Toolset
├── exception/
│   ├── HermesException.java
│   ├── HermesApiException.java
│   ├── HermesAuthException.java
│   └── HermesNetworkException.java
├── logging/
│   ├── HermesLogger.java       — 统一日志入口
│   └── LogEvents.java          — 日志事件码
├── sdk/
│   └── HermesSdk.java          — 业务封装
│       ├── SessionManager     — 会话管理
│       ├── ToolsetManager     — Toolset 查询
│       ├── SkillExecutor      — Skill 执行
│       └── SystemMonitor      — 系统监控
└── service/
    ├── SkillService.java       — Skill 便捷调用
    ├── ApiServerService.java   — API Server 服务
    ├── SessionService.java     — 会话服务
    └── RunService.java         — Run 服务

src/main/resources/
├── application.yml
└── log4j2.xml
```

## 依赖版本

| 依赖 | 版本 |
|------|------|
| Log4j2 | 2.24.1 |
| SLF4J | 2.0.16 |
| OkHttp | 4.12.0 |
| Jackson | 2.17.2 |
| Lombok | 1.18.34 |
| JUnit | 5.11.0 |
| Spring Boot | 3.3.x |
| Java | 17+ |