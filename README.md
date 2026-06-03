# Hermes Java SDK

[![Maven](https://img.shields.io/badge/Maven-1.0.0-blue)](https://search.maven.org/artifact/com.hermes/hermes-sdk)
[![Java](https://img.shields.io/badge/Java-17%2B-brightgreen)](https://www.oracle.com/java/technologies/javase/17-relnote-issues.html)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Tests](https://img.shields.io/badge/Tests-80%20passed-brightgreen)](#测试覆盖)

Hermes Agent 的 Java SDK，支持 Spring Boot 3.x，提供工业级鲁棒性、线程安全、传输层可扩展。

## 快速开始

### Maven 依赖

```xml
<dependency>
    <groupId>com.hermes</groupId>
    <artifactId>hermes-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 使用示例

```java
// 创建客户端（只需 baseUrl）
HermesClient hermes = HermesClient.builder()
    .baseUrl("http://localhost:8080")
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

## 测试覆盖

**80 tests，覆盖 9 个模块：**

| 模块 | 测试数 | 覆盖内容 |
|------|--------|---------|
| Config | 4 | Builder 默认值/自定义、不可变性 |
| Exception | 6 | 4层异常体系 |
| DTO | 7 | Jackson 序列化/反序列化 |
| Client | 11 | Builder、参数校验、健康检查 |
| SessionService | 11 | CRUD + 成功/错误场景 |
| RunService | 8 | 异步 Run + 审批流程 |
| SkillService | 11 | execute/async + 便捷方法 |
| **Security** | **14** | 输入校验、XSS/SQL 注入防御 |
| **Performance** | **8** | 超时、并发、大响应、重试 |

```
mvn test   # 运行所有测试
mvn test -Dtest=SessionServiceTest  # 运行指定测试类
```

## Troubleshooting

### 连接失败

```
HermesNetworkException: Failed to connect to localhost/127.0.0.1:8080
```

**检查项：**
1. Hermes Gateway 是否运行（默认 `http://localhost:8080`）
2. 防火墙是否允许 8080 端口
3. baseUrl 是否正确（末尾不带 `/`）

```java
// 正确
hermes = HermesClient.builder().baseUrl("http://localhost:8080").build();

// 错误
hermes = HermesClient.builder().baseUrl("http://localhost:8080/").build();
```

### 超时错误

```
HermesNetworkException: timeout
```

**解决：** 调整超时时间

```java
HermesClient hermes = HermesClient.builder()
    .baseUrl("http://localhost:8080")
    .connectTimeout(10)  // 连接超时（秒）
    .readTimeout(300)     // 读取超时（秒）
    .build();
```

### 401 认证错误

```
HermesApiException: AUTH_ERROR (401)
```

**说明：** SDK 本身不需要认证，检查 Hermes Gateway 的认证配置。

### 500 服务器错误

```
HermesApiException: API_ERROR (500): Internal Server Error
```

**排查：**
1. 查看 Hermes Gateway 日志
2. 检查请求参数是否合法
3. 确认 backend 服务健康状态

### 异常捕获示例

```java
try {
    String result = hermes.chat("你好");
} catch (HermesNetworkException e) {
    // 网络问题（超时、连接失败）
    System.err.println("网络错误: " + e.getMessage());
} catch (HermesApiException e) {
    // API 返回错误（400/401/403/404/500）
    System.err.println("API 错误: " + e.getErrorCode() + " - " + e.getMessage());
}
```

### 线程安全

```java
// 多线程共享同一客户端 — 安全 ✓
HermesClient client = HermesClient.builder().baseUrl("http://localhost:8080").build();
ExecutorService executor = Executors.newFixedThreadPool(10);
for (int i = 0; i < 10; i++) {
    executor.submit(() -> client.chat("并发请求"));
}

// 多轮对话 — 使用 ThreadSafeChatSession
ThreadSafeChatSession session = client.newThreadSafeSession();
session.chat("第一轮");
session.chat("第二轮"); // 线程安全，不会混淆上下文
```

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
│   ├── HermesNetworkException.java
│   └── HermesAuthException.java
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

## Contributing

### 开发环境

```bash
# 克隆
git clone https://github.com/zxs9999/hermes-java-sdk.git
cd hermes-java-sdk

# 编译
JAVA_HOME=/home/jack/opt/jdk-17.0.12+7 \
  /mnt/d/tools/apache-maven-3.9.9/bin/mvn clean compile

# 测试
JAVA_HOME=/home/jack/opt/jdk-17.0.12+7 \
  /mnt/d/tools/apache-maven-3.9.9/bin/mvn test

# 运行示例
JAVA_HOME=/home/jack/opt/jdk-17.0.12+7 \
  /mnt/d/tools/apache-maven-3.9.9/bin/mvn exec:java \
  -Dexec.mainClass=com.hermes.sdk.Demo
```

### 测试规范

- 所有新增功能必须有对应的单元测试
- 测试覆盖率不低于 80%
- 使用 Mockito mock 外部依赖（OkHttpClient）
- 性能测试（超时、并发）使用真实网络模拟

```java
// 测试分类
src/test/java/com/hermes/sdk/
├── config/     — 配置层测试
├── exception/  — 异常层测试
├── dto/        — DTO 序列化测试
├── client/     — 客户端测试
├── service/    — Service 层测试（mock HTTP）
├── security/   — 安全测试（注入、校验、脱敏）
└── performance/ — 性能测试（超时、并发）
```

### 代码规范

- 命名：简洁、有意义、无缩写歧义
- 异常：细分层次，精准捕获
- 日志：统一事件码，便于追踪
- 文档：Javadoc 注释所有 public 方法

## Changelog

### [1.0.0] - 2026-06-03

**新增**
- 分层架构：Core / Api / Sdk / Client 四层
- HermesClient 主客户端（聊天 + Skill 激活）
- HermesSdk 业务封装（会话管理 / Toolset / 系统监控）
- HermesApi 原始 API（23个端点）
- Transport 接口（HttpTransport 已实现，RPC/WebSocket 待实现）
- 异常体系：HermesException → Network / Api / Auth
- 安全测试：14 tests（输入校验、XSS/SQL 注入防御）
- 性能测试：8 tests（超时、并发、大响应）

**修复**
- 移除 Lombok 依赖（手动生成 getter/setter）
- 修复 JsonProcessingException 受检异常处理
- 修复 HermesConfig 构造函数可见性

**测试**
- 80 tests，全部通过
- 覆盖 Config / Exception / DTO / Client / Service / Security / Performance