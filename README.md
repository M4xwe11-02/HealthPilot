<div align="center">

<pre style="display: inline-block; text-align: left;">
 ██╗  ██╗███████╗ █████╗ ██╗  ████████╗██╗  ██╗
 ██║  ██║██╔════╝██╔══██╗██║  ╚══██╔══╝██║  ██║
 ███████║█████╗  ███████║██║     ██║   ███████║
 ██╔══██║██╔══╝  ██╔══██║██║     ██║   ██╔══██║
 ██║  ██║███████╗██║  ██║███████╗██║   ██║  ██║
 ╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝╚══════╝╚═╝   ╚═╝  ╚═╝

 ██████╗ ██╗██╗      ██████╗ ████████╗
 ██╔══██╗██║██║     ██╔═══██╗╚══██╔══╝
 ██████╔╝██║██║     ██║   ██║   ██║&nbsp;&nbsp;&nbsp;
 ██╔═══╝ ██║██║     ██║   ██║   ██║&nbsp;&nbsp;&nbsp;
 ██║     ██║███████╗╚██████╔╝   ██║&nbsp;&nbsp;&nbsp;
 ╚═╝     ╚═╝╚══════╝ ╚═════╝    ╚═╝&nbsp;&nbsp;&nbsp;
</pre>

### ✦ AI-Powered Health Management Platform ✦

[![Java](https://img.shields.io/badge/Java%2021-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://react.dev/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-00B4D8?style=for-the-badge)](https://github.com/M4xwe11-02/HealthPilot/blob/master/LICENSE)

<br/>

**Spring Boot · React · PostgreSQL/pgvector · Redis · MinIO · LightRAG**

A full-stack health management platform featuring dual-engine RAG,
SSE streaming, message-queue decoupling, and token-level access control.

</div>

---

## ◈ Core Capabilities

<table>
<tr>
<td width="50%">

### ① Dual-Engine RAG System

Built-in engine powered by **Spring AI + pgvector HNSW** with query rewriting, dynamic top-K, similarity thresholds, metadata filtering, and short-query hit validation. **LightRAG** provides a switchable graph-enhanced retrieval alternative for knowledge-intensive queries.

</td>
<td width="50%">

### ② Message Queue Decoupling

MQ-based architecture with **Consumer Groups** for horizontal scaling. Automatic retry with configurable limits, end-to-end task state tracking, and fault-tolerant processing ensure reliable throughput under load.

</td>
</tr>
<tr>
<td width="50%">

### ③ SSE Streaming Output

**Project Reactor Flux** pushes LLM responses token-by-token to the frontend. `doOnNext` / `takeWhile` operators enable early stream termination, saving tokens and reclaiming resources in real time.

</td>
<td width="50%">

### ④ Knowledge Base Pipeline

Custom text cleaning (HTML removal, whitespace normalization), **SHA-256 content hashing** for cross-knowledge-base deduplication, batch vectorization with **DashScope 1024-dim embeddings**, and per-user metadata-isolated retrieval.

</td>
</tr>
<tr>
<td width="50%">

### ⑤ Token-Based Security

**SecureRandom** 32-byte tokens, Base64 URL-safe encoding, SHA-256 hashing before storage. Per-user DB query filtering prevents token enumeration attacks. Every request is independently verified.

</td>
<td width="50%">

### ⑥ Multi-Dimensional Rate Limiting

Per-user rate limiting with **graceful degradation** on core interfaces. Protects against abuse while maintaining service availability during high-concurrency traffic.

</td>
</tr>
<tr>
<td width="50%">

### ⑦ S3-Compatible Object Storage

Unified abstraction over **MinIO** (dev) and **RustFS** (prod). Date-tiered + UUID file keys, automatic filename normalization for cross-locale compatibility, and seamless environment switching.

</td>
<td width="50%">

### ⑧ Virtual Threads & Caching

**Java 21 virtual threads** maximize I/O-bound throughput. **Redis Cache-Aside** strategy on public query endpoints reduces database load and improves response latency.

</td>
</tr>
</table>

---

## ◈ Architecture

| Layer | Component | Stack |
|:------|:----------|:------|
| **Presentation** | Frontend SPA | React · TypeScript · pnpm · Vite |
| | SSE Streaming UI | Project Reactor Flux · EventSource |
| **Application** | Backend API | Spring Boot · Spring AI · Gradle |
| | Concurrency | Java 21 Virtual Threads |
| | Async Processing | MQ Consumers · Retry · State Tracking |
| | RAG Pipeline | Query Rewrite · Dynamic TopK · Metadata Filter |
| **Data** | Vector Store | PostgreSQL · pgvector (HNSW) |
| | Cache | Redis · Cache-Aside Strategy |
| | Object Storage | MinIO (dev) / RustFS (prod) · S3 API |
| | Graph Retrieval | LightRAG · Knowledge Graph |
| **External** | LLM & Embeddings | Alibaba Cloud Bailian API · DashScope 1024-dim |

---

## ◈ Quick Start

> **Prerequisites:** JDK 21, Docker Compose, Node.js, Alibaba Cloud Bailian API Key

### Clone & Configure

```bash
git clone https://github.com/M4xwe11-02/HealthPilot.git
cd HealthPilot
cp .env.example .env
cp lightrag/.env.example lightrag/.env
```

Edit `.env` — set your API key:

```env
AI_BAILIAN_API_KEY=your_bailian_api_key_here
```

Edit `lightrag/.env` — replace both placeholder keys:

```env
LLM_BINDING_API_KEY=your_bailian_api_key_here
EMBEDDING_BINDING_API_KEY=your_bailian_api_key_here
```

The root `.env` enables the backend-to-LightRAG integration by default through
`APP_LIGHTRAG_ENABLED=true` and `APP_LIGHTRAG_BASE_URL=http://localhost:9621`.

### Launch Infrastructure

```bash
docker compose up -d postgres redis minio createbuckets lightrag
docker compose ps
```

### Start Backend

```bash
# Unix / macOS
set -a && source .env && set +a
./gradlew :app:bootRun --no-daemon --console=plain

# Windows (PowerShell)
Get-Content .env | ForEach-Object {
  $line = $_.Trim()
  if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
    $name, $value = $line -split "=", 2
    [Environment]::SetEnvironmentVariable($name.Trim(), $value.Trim().Trim('"').Trim("'"), "Process")
  }
}
.\gradlew.bat :app:bootRun --no-daemon --console=plain
```

### Start Frontend

```bash
corepack enable
corepack pnpm --dir frontend install
corepack pnpm --dir frontend dev -- --host 0.0.0.0 --port 5173 --strictPort
```

Open **http://localhost:5173**. The development admin account is initialized as `admin` / `admin`.

---

## ◈ Service Map

| Service | Dev Address | Notes |
|:--------|:------------|:------|
| **Frontend** | `localhost:5173` | Vite dev server; production on `:80` |
| **Backend API** | `localhost:8081` | Spring Boot with virtual threads |
| **PostgreSQL** | `localhost:5433` | Container internal `5432` → host `5433` |
| **Redis** | `localhost:6380` | Cache-Aside for public endpoints |
| **MinIO API** | `localhost:9000` | S3-compatible object storage |
| **MinIO Console** | `localhost:9001` | Web management UI |
| **LightRAG** | `localhost:9621` | Graph-enhanced retrieval engine |

---

## ◈ Configuration

| File | Purpose |
|:-----|:--------|
| `.env` | Main project config — used by local backend and Docker Compose |
| `lightrag/.env` | LightRAG model and embedding provider config |

| Variable | Required | Purpose |
|:---------|:---------|:--------|
| `AI_BAILIAN_API_KEY` | Yes | Backend LLM and embedding access |
| `APP_LIGHTRAG_ENABLED` | For graph RAG | Enables LightRAG-backed retrieval |
| `APP_LIGHTRAG_BASE_URL` | For graph RAG | Backend URL for the LightRAG HTTP service |
| `LLM_BINDING_API_KEY` | For LightRAG | LightRAG LLM provider key |
| `EMBEDDING_BINDING_API_KEY` | For LightRAG | LightRAG embedding provider key |

Both `.env` files are git-ignored. Never commit secrets to version control.

### Diagnostics

Use these checks to verify the local runtime:

```bash
docker compose ps
docker compose logs -f lightrag
curl http://localhost:9621/
curl http://localhost:8081/api/auth/me
```

Expected signals: LightRAG returns a redirect to `/webui/`; the backend auth probe returns a JSON `401` response when unauthenticated.

---

<div align="center">

**Built with precision. Powered by AI. Designed for health.**

`HealthPilot` · `v0.1` · `MIT License`

</div>
