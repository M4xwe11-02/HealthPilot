# 项目演示视频
https://www.bilibili.com/video/BV1BYd9BREZa/?share_source=copy_web&vd_source=3ca0ef37fbaa8515fb2e86b92bcce510
---
# HealthPilot V0.1
Spring Boot + React + PostgreSQL/pgvector + Redis + MinIO + LightRAG 的 AI 健康管理平台。

## 项目亮点

- 利用 MQ 机制解耦请求与处理，结合 Consumer Group 实现水平扩展，并支持最多三次自 动重试与任务状态全链路追踪保障用户体验与任务可靠性。
- 构建双引擎 RAG 问答质量保障机制：内置引擎基于 Spring AI + pgvector HNSW 索引，结 合 QueryRewrite、 动态 topK 与相似度阈值（依查询长度自适应切换检索参数）、知识库前置元 数据过滤、短 Query 命中校验与 流式探针检测，降低误召回与无依据回答风险；同时接入 LightRAG 作为可替换的图增强检索引擎，支持用户 自由切换问答链路，兼顾召回质量与灵活 性。
- 支持多维度个性化组合限流，低侵入式接入核心接口，并配置优雅降级方法，保障系统在高 并发场景下的稳定性与可用性。
- 配合自定义文本清洗管道（HTML 去除、空白归一化、换行标准化），通过 SHA-256 内容 哈希实现跨知识库去重复用， 避免冗余向量化与 AI 调用；向量化采用 DashScope 1024 维 Embedding，分批写入 pgvector，支持多用户 知识库元数据隔离检索。 
- 基于 Java 21 虚拟线程全面提升 I/O 密集型场景吞吐量，结合 Project Reactor Flux 实现 SSE 流式输出， 支持 LLM 答案逐字推送至前端，并通过 doOnNext/takeWhile 算子实现流的早期终止 与资源回收， 避免无效 Token 消耗，提升长连接场景下的响应流畅度。
- 设计 S3 兼容的对象存储抽象层，统一对接 MinIO（开发）与 RustFS（生产），文件 Key 采 用 日期分层 + UUID 命名，文件名通过 pinyin4j 将中文转拼音后清洗特殊字符，保障跨平台兼容 性； 上传、下载、删除、大小探测全链路封装，业务层零感知底层存储实现。
- 实现 Token 级安全认证机制：使用 SecureRandom 生成 32 字节随机令牌，Base64 URL Safe 编码后 对外暴露，存储前经 SHA-256 哈希处理，结合用户维度的数据库查询过滤，防止越 权访问与 Token 枚举攻击， 保障多用户数据隔离。
- 公共查询接口均引入 Redis 缓存层，大幅提升高并发下的查询性能，提升系统吞吐， Cache-Aside策略保证不频繁更新的公共文档的数据一致性。

## 端口

| 服务 | 地址 |
| --- | --- |
| 前端 | `http://localhost:5173`（开发）/ `http://localhost:80`（生产） |
| 后端 API | `http://localhost:8081` |
| PostgreSQL | `localhost:5433` |
| Redis | `localhost:6380` |
| MinIO API | `http://localhost:9000` |
| MinIO 控制台 | `http://localhost:9001` |
| LightRAG | `http://localhost:9621` |

> PostgreSQL 容器内部仍然是 `5432`，映射到宿主机 `5433`，避免和本机已有 PostgreSQL 冲突。

---

## Windows 从零启动

以下命令在 **PowerShell** 中执行。

### 1. 拉代码

```powershell
git clone <你的仓库地址> health-guardian
cd health-guardian
```

### 2. 安装 JDK 21

```powershell
winget install --id EclipseAdoptium.Temurin.21.JDK -e
```

关闭 PowerShell，重新打开后确认版本：

```powershell
java -version
```

需要看到 `21`。

### 3. 创建配置文件

**主配置（必须）：**

```powershell
Copy-Item .env.example .env
notepad .env
```

至少把这一行改成自己的 key：

```env
AI_BAILIAN_API_KEY=你的百炼APIKey
```

**LightRAG 配置（必须）：**

```powershell
Copy-Item lightrag\.env.example lightrag\.env
notepad lightrag\.env
```

把两处 `replace_with_your_api_key` 都改成同一个百炼 API Key：

```env
LLM_BINDING_API_KEY=你的百炼APIKey
EMBEDDING_BINDING_API_KEY=你的百炼APIKey
```

### 4. 启动所有依赖

```powershell
docker compose up -d postgres redis minio createbuckets lightrag
docker compose ps
```

确认各容器状态为 `running`：

```powershell
docker compose ps
```

检查数据库连通性：

```powershell
Test-NetConnection 127.0.0.1 -Port 5433
```

`TcpTestSucceeded` 应该是 `True`。

### 5. 启动后端

```powershell
Get-Content .env | ForEach-Object {
  $line = $_.Trim()
  if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
    $name, $value = $line -split "=", 2
    [Environment]::SetEnvironmentVariable($name.Trim(), $value.Trim().Trim('"').Trim("'"), "Process")
  }
}

.\gradlew.bat :app:bootRun --no-daemon --console=plain
```

后端启动后访问 `http://localhost:8081` 应返回响应。

### 6. 启动前端

另开一个 PowerShell：

```powershell
corepack enable
corepack pnpm --dir frontend install
corepack pnpm --dir frontend dev -- --host 0.0.0.0 --port 5173 --strictPort
```

访问 `http://localhost:5173`。

---

## Git Bash / Linux / macOS 启动后端

```bash
cp .env.example .env
cp lightrag/.env.example lightrag/.env
# 编辑两个 .env，填入 API Key
set -a && source .env && set +a
./gradlew :app:bootRun --no-daemon --console=plain
```

---

## 配置说明

| 文件 | 用途 |
| --- | --- |
| `.env` | 主项目配置，本地后端启动和 Docker Compose 都用它 |
| `lightrag/.env` | LightRAG 容器配置，只有 LightRAG 容器读取 |

两个 `.env` 文件都不要提交到 Git。

---

## 常见问题

### 5433 端口被占用

不需要改代码。项目默认宿主机 `5433`，可在 `.env` 中修改 `POSTGRES_HOST_PORT`：

```env
POSTGRES_HOST_PORT=5434
```

### 数据库密码错误

PostgreSQL 账号密码只在数据卷首次初始化时生效。如果曾用旧密码启动过，需要重建卷：

```powershell
docker compose down -v
docker compose up -d postgres redis minio createbuckets lightrag
```

### Gradle 下载超时

项目默认使用腾讯云镜像。如果仍然超时，切换为华为云：

```powershell
(Get-Content .\gradle\wrapper\gradle-wrapper.properties) `
  -replace 'mirrors.cloud.tencent.com/gradle', 'mirrors.huaweicloud.com/gradle' |
  Set-Content .\gradle\wrapper\gradle-wrapper.properties
```

### LightRAG 容器启动失败

检查 `lightrag/.env` 是否已正确填写 API Key：

```powershell
docker compose logs lightrag
```

### 后端启动时报 API Key 缺失

```powershell
notepad .env
```

确认 `AI_BAILIAN_API_KEY` 已改成自己的百炼 API Key，不要保留 `replace_with_your_api_key`。

---

----

# 注意：

如果要用公共查询功能，需要使用管理员账号登录，需要管理员账号对文件进行上传和撤回：

账号：admin

密码：admin
