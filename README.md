# Hermes Java SDK

Hermes Agent 的 Java SDK，支持 Spring Boot 3.x，提供工业级鲁棒性、线程安全、传输层可扩展。

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
│  Transport 接口                              │
│    ├─ HttpTransport     ✓ 当前实现          │
│    ├─ RpcTransport      ○ 占位（RPC 待实现） │
│    └─ WebSocketTransport ○ 占位（WS 待实现） │
├─────────────────────────────────────────────┤
│  OkHttp / Log4j2 / Jackson — 底层依赖        │
└─────────────────────────────────────────────┘
```

## 功能特性

| 特性 | 说明 |
|------|------|
| **分层设计** | Core/Api/Sdk/Client 四层分离，职责清晰 |
| **传输层抽象** | Transport 接口，支持 HTTP/RPC/WebSocket 切换 |
| **异常细分** | HermesException → Network / Api 两类，可精准处理 |
| **重试机制** | 指数退避（1s → 2s → 4s），最多 3 次 |
| **线程安全** | ThreadSafeChatSession / CopyOnWriteArrayList |
| **异步支持** | CompletableFuture + thenAccept |
| **日志框架** | Log4j2 2.24.1 + SLF4J，统一事件码 |
| **极简配置** | 只需 baseUrl，无需 apiKey / model |

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
```

### 3. 使用示例

```java
// 创建客户端（只需 baseUrl）
HermesClient hermes = HermesClient.builder()
    .baseUrl(System.getenv("HERMES_BASE_URL"))
    .build();

// 聊天
String result = hermes.chat("你好，Hermes！");

// 激活 Skill
String result = hermes.activateSkill("karpathy-principles", "重构 auth 模块");

// 线程安全多轮对话
ThreadSafeChatSession session = hermes.newThreadSafeSession();
session.chat("我想写小说");
session.chat("都市异能题材");
String novel = session.chat("开始写第1章");

// 健康检查
if (hermes.healthCheck()) {
    System.out.println("连接正常");
}
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

### Transport（传输层）

可替换的传输层实现：

```java
// HTTP（默认）
Transport transport = new HttpTransport(config, httpClient, mapper);

// 切换到 WebSocket（未来）
HermesClient hermes = HermesClient.builder()
    .baseUrl("wss://api.hermes.com")
    .transportType(TransportType.WEBSOCKET)
    .build();

// 切换到 RPC（未来）
HermesClient hermes = HermesClient.builder()
    .baseUrl("localhost:50051")
    .transportType(TransportType.RPC)
    .build();
```

## 传输层扩展

| 类型 | 状态 | 说明 |
|------|------|------|
| **HttpTransport** | ✓ 已实现 | 基于 OkHttp，当前默认 |
| **RpcTransport** | ○ 占位符 | gRPC/Thrift 待实现 |
| **WebSocketTransport** | ○ 占位符 | SSE/WebSocket 待实现 |

未来扩展只需：
1. 实现 `Transport` 接口
2. 在 `HermesClient.Builder` 选择传输类型

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
│   └── HermesApi.java          — 原始 API
├── dto/
│   ├── ChatRequest.java
│   ├── Message.java
│   ├── Run.java
│   ├── Session.java
│   ├── Skill.java
│   ├── SkillRequest.java
│   └── Toolset.java
├── exception/
│   ├── HermesException.java
│   ├── HermesApiException.java
│   └── HermesNetworkException.java
├── logging/
│   ├── HermesLogger.java
│   └── LogEvents.java
├── sdk/
│   └── HermesSdk.java          — 业务封装
├── service/
│   ├── SkillService.java
│   ├── ApiServerService.java
│   ├── SessionService.java
│   └── RunService.java
└── transport/
    ├── Transport.java          — 接口定义
    ├── TransportType.java      — HTTP/RPC/WEBSOCKET
    ├── HttpTransport.java       — 当前实现
    ├── RpcTransport.java       — 占位符
    └── WebSocketTransport.java — 占位符

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