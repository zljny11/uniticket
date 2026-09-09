# UniTicket

**Completed campus ticketing platform with high-concurrency order processing and AI-powered event discovery.**

Java 8 · Spring Boot · MySQL · Redis · Kafka · Caffeine · Lua · Docker

## Overview

UniTicket is an API-first platform for campus venue browsing, ticket publishing, authentication, and high-concurrency ordering. It separates fast request admission from database persistence and uses Redis, Kafka, and scheduled reconciliation to maintain order and inventory consistency.

## Architecture

~~~mermaid

flowchart TD
    C["Web / Mobile Client"] --> N["Nginx"]
    N --> API["Spring Boot REST API"]
    API --> G["JWT Authentication<br/>Layered Rate Limiting"]

    G --> READ["Venue & Event Queries"]
    READ --> L1["Caffeine L1 Cache"]
    L1 --> L2["Redis L2 Cache"]
    L2 --> DB["MySQL"]

    G --> ORDER["Flash-Sale Ordering"]
    ORDER --> LUA["Redis Lua<br/>Stock + One-Order Check"]
    LUA --> K["Kafka"]
    K --> W["Transactional Order Consumer"]
    W --> DB

    JOB["Scheduled Reconciliation"] --> DB
    JOB --> L2

    API --> AI["Campus-AI FastAPI"]
    AI --> F["Ollama Embeddings + FAISS"]
    AI --> LLM["DeepSeek-V3"]
~~~

## Key Features

### High-concurrency ticket ordering

- Redis Lua atomically checks inventory, enforces one-order-per-user, and reserves stock.
- Accepted requests are submitted to Kafka for asynchronous MySQL order creation.
- Manual Kafka acknowledgement and a database unique index make processing idempotent.
- A scheduled reconciliation job closes incomplete unpaid orders and restores inventory.

### Multi-level caching

- Caffeine L1 + Redis L2 for venue and event reads.
- Cache penetration protection, logical expiration, background rebuild, invalidation, and preheating.

### Layered traffic protection

- Interface-level Redis token bucket.
- User- and IP-level Redis sliding-window limits.
- Configurable thresholds for ordering and venue endpoints.

### AI event assistant

UniTicket integrates the separately deployed [Campus-AI](https://github.com/zljny11/Campus-AI) FastAPI service through REST APIs. It uses Ollama embeddings, FAISS retrieval, metadata-aware context selection, and DeepSeek generation.

On a fixed evaluation set, retrieval improvements increased **Ragas Faithfulness from 0.62 to 0.81**.

## Order Flow

~~~text
Request
  -> rate-limit validation
  -> Redis Lua inventory and duplicate check
  -> distributed order ID
  -> Kafka order event
  -> transactional MySQL persistence
  -> timeout reconciliation when required
~~~

## Performance

The asynchronous order endpoint was benchmarked with JMeter in a **2 vCPU / 4 GB Docker environment**:

| Throughput | Average response time | Correctness |
|---|---:|---|
| **1,000+ RPS** | **~60 ms** | No overselling or duplicate orders |

Redis inventory, MySQL inventory, and persisted order counts were reconciled after each run.

## Quick Start

Requirements: JDK 8+, Maven, MySQL, Redis, and Docker Compose.

~~~bash
git clone https://github.com/zljny11/uniticket.git
cd uniticket/项目

mysql -u root -p uniticket < ../uniticket.sql
docker compose up -d
mvn spring-boot:run
~~~

The API starts on http://localhost:8081; Kafka is exposed on localhost:19092.

## Main Endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| POST | /auth/login | User login |
| GET | /venue/{id} | Query venue |
| POST | /ticket/flash-sale | Publish flash-sale ticket |
| POST | /ticket-order/flash-sale/{ticketId} | Submit ticket order |
| POST | http://localhost:8000/ai/ask | Ask the AI assistant |

## Repository Structure

~~~text
项目/
├── src/main/java/com/uniticket/
│   ├── controller/    REST APIs
│   ├── service/       order and cache logic
│   ├── mq/            Kafka consumers
│   ├── ratelimit/     traffic protection
│   └── job/           reconciliation jobs
├── src/main/resources/
│   ├── mapper/        Redis Lua scripts
│   └── db/            database scripts
├── docker-compose.yml
└── pom.xml
~~~

## Core Implementations

- [Asynchronous order service](项目/src/main/java/com/uniticket/service/impl/VoucherOrderServiceImpl.java)
- [Atomic Redis Lua admission](项目/src/main/resources/mapper/seckill.lua)
- [Kafka order consumer](项目/src/main/java/com/uniticket/mq/SeckillVoucherConsumer.java)
- [Scheduled reconciliation](项目/src/main/java/com/uniticket/job/OrderAutoCloseJob.java)
- [Caffeine + Redis cache](项目/src/main/java/com/uniticket/service/MultiLevelCacheService.java)
- [Rate limiting](项目/src/main/java/com/uniticket/ratelimit/RateLimiterService.java)

