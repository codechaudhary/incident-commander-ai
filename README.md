# 🚨 Incident Commander AI

> **AI-powered production incident management platform** — ingests distributed traces, detects anomalies, identifies root causes using multi-agent AI, and auto-generates postmortem reports. Built with Java Spring Boot, Python FastAPI, Kafka, and deployed on Azure.

[![Java](https://img.shields.io/badge/Java-17-blue?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-6DB33F?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Python](https://img.shields.io/badge/Python-3.11-3776AB?style=flat-square&logo=python)](https://python.org)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.110-009688?style=flat-square&logo=fastapi)](https://fastapi.tiangolo.com/)
[![Kafka](https://img.shields.io/badge/Apache_Kafka-3.7-231F20?style=flat-square&logo=apachekafka)](https://kafka.apache.org/)
[![React](https://img.shields.io/badge/React-18-61DAFB?style=flat-square&logo=react)](https://react.dev/)
[![Azure](https://img.shields.io/badge/Azure-Container_Apps-0078D4?style=flat-square&logo=microsoftazure)](https://azure.microsoft.com/)

---

## 📋 Table of Contents

- [What it does](#-what-it-does)
- [Architecture](#-architecture)
- [Tech stack](#-tech-stack)
- [Services](#-services)
- [Key engineering decisions](#-key-engineering-decisions)
- [Project structure](#-project-structure)
- [Getting started](#-getting-started)
- [Kafka event contracts](#-kafka-event-contracts)
- [API reference](#-api-reference)
- [Azure deployment](#-azure-deployment)
- [Demo](#-demo)
- [What I would do differently](#-what-i-would-do-differently)
- [Authors](#-authors)

---

## 🔍 What it does

Large engineering teams lose hours during production incidents manually searching logs, metrics, dashboards, and past tickets. **Incident Commander AI** acts as an automated first responder:

1. **Ingests** distributed microservice traces and span trees via REST
2. **Detects anomalies** in real-time using z-score statistical analysis against rolling baselines
3. **Identifies root cause** using a multi-agent AI pipeline (log analyzer → root cause agent → fix recommendation agent)
4. **Generates postmortem reports** automatically using retrieval-augmented generation (RAG) over past incidents
5. **Alerts** on-call engineers via real-time WebSocket push with severity-ranked alerts
6. **Displays** everything on a live React dashboard — DAG service graph, span waterfall, AI narration panel, alert center

### User flow

```
Engineer POSTs a trace  →  Kafka triggers AI analysis  →  Root cause identified
       ↓                                                          ↓
  DAG graph turns red   ←   WebSocket push to dashboard  ←  Alert fires
       ↓
  Click trace → see span waterfall + AI narration + similar past incidents
       ↓
  One-click postmortem report generated
```

---

## 🏗️ Architecture

### High-level

```
┌─────────────────────────────────────────────────────┐
│                   React SPA                         │
│    Dashboard · Traces · Alerts · DAG Graph          │
└─────────────────────┬───────────────────────────────┘
                      │  REST + WebSocket
┌─────────────────────▼───────────────────────────────┐
│           BFF / API Gateway                         │
│      Spring WebFlux · Resilience4j · Nginx          │
│   (parallel fanout, circuit breaker, WS relay)      │
└──┬──────────────┬────────────────┬──────────────────┘
   │              │                │
   ▼              ▼                ▼
[Java Services]  [Kafka Bus]   [Python Services]
Trace Ingestion  ──────────►  AI Narration Engine
Incident Service ◄──────────  Anomaly Detection
Alert Service                  RAG + Vector Search
Auth Service
   │                                  │
   ▼                                  ▼
[PostgreSQL ×4]              [PostgreSQL + pgvector]
[Redis Cache]
```

### Kafka event flow

```
Trace Ingestion ──► trace.ingested ──► AI Narration Engine
                                              │
                                              ▼
                                       narration.ready ──► BFF ──► WebSocket

Anomaly Detection ──► anomaly.detected ──► Alert Service
                                                │
                                                ▼
                                          alert.fired ──► BFF ──► WebSocket
```

---

## 🛠️ Tech stack

| Layer | Technology | Why |
|---|---|---|
| Java backend | Spring Boot 3.2, Spring WebFlux | Reactive BFF with non-blocking parallel calls |
| Java AI integration | Spring AI | Native LLM calls and embeddings from Java |
| Python backend | FastAPI, asyncio | Async AI pipelines, Kafka subscribers |
| AI / LLM | Anthropic Claude (via API) | Root cause narration, severity classification |
| Event streaming | Apache Kafka | Durable, replayable, ordered event delivery |
| Cache / pub-sub | Redis | Narration caching, WebSocket relay |
| Primary databases | PostgreSQL ×4 | Database-per-service pattern |
| Vector search | pgvector extension | Semantic similarity search over past incidents |
| Resilience | Resilience4j | Circuit breaker on AI service calls |
| Observability | OpenTelemetry + Azure App Insights | Distributed trace correlation across all services |
| Frontend | React 18, TailwindCSS, Recharts, React Flow | Real-time dashboard, DAG graph, span waterfall |
| State management | Zustand + TanStack Query | Local UI state + server cache |
| Infrastructure | Azure Container Apps, GitHub Actions | CI/CD, managed scaling, zero K8s overhead |

---

## 📦 Services

### Java services (owned by [@your-github-username])

#### `bff-service` — API Gateway / Backend for Frontend
The most architecturally interesting Java service. Uses **Spring WebFlux** (reactive, non-blocking) to fan out parallel requests to all 4 downstream services simultaneously and merge them into a single dashboard response in under 200ms.

Key components:
- `DashboardAggregator` — `Mono.zip()` parallel fanout across services
- `LiveTraceHandler` — WebSocket server that subscribes to Redis and fans out all events to connected UI clients in real-time
- **Resilience4j circuit breakers** on the AI narration client — if the Python AI service goes down, the dashboard degrades gracefully and serves cached narrations instead of returning errors
- Typed `WebClient` instances per downstream service

#### `trace-ingestion-service` — Span tree ingestion
Accepts distributed trace payloads, walks span trees to compute durations and detect error statuses, upserts service dependency relationships, and publishes `trace.ingested` events to Kafka.

Key components:
- `SpanTreeWalker` — recursive span tree traversal
- `ServiceDependencyUpdater` — upsert call count, error count, p99 latency per service pair
- `TraceEventPublisher` — Kafka producer

#### `incident-service` — Incident lifecycle + Spring AI
Manages incident lifecycle from open to resolved. Integrates **Spring AI directly** to call Claude for a fast severity classification and to generate embeddings for semantic similarity search over past incidents.

Key components:
- `ChatClient` (Spring AI) — calls Claude to classify severity (LOW/MEDIUM/HIGH/CRITICAL) from incident description
- `EmbeddingClient` (Spring AI) — generates vector embeddings for semantic search
- `SimilarIncidentFinder` — queries pgvector for top-k similar past incidents
- `TimelineBuilder` — constructs chronological incident event timeline
- `MTTRCalculator` — tracks mean time to resolution

#### `alert-service` — Rules-based alerting
Listens to `anomaly.detected` Kafka events, evaluates them against configurable alert rules, fires alerts when thresholds are breached, and exposes a REST API for alert management.

Key components:
- `AnomalyEventConsumer` — Kafka consumer group
- `AlertEvaluator` — rules engine (error_rate > 10%, p99 > 1000ms, custom rules)
- Default alert rules seeded on startup

#### `auth-service` — Authentication
JWT-based authentication and authorization using Spring Security.

---

### Python services (owned by [@friend-github-username])

#### `ai-narration-service` — LLM root cause analysis
Listens to `trace.ingested` Kafka events and asynchronously triggers LLM-powered root cause analysis. Builds structured prompts from span trees, validates responses with Pydantic schemas, caches results in Redis, and publishes `narration.ready`.

#### `anomaly-detection-service` — Statistical anomaly detection
Maintains rolling 1-hour baselines (mean + stddev) per service metric. Uses z-score analysis (z > 3 = anomaly) to detect and classify anomalies as MEDIUM/HIGH/CRITICAL. Publishes `anomaly.detected` to Kafka.

#### `rag-service` — Vector search and postmortem generation
Generates and stores embeddings for every resolved incident. Exposes semantic similarity search so the Incident Service can find and surface past similar incidents and their resolutions.

---

## 🧠 Key engineering decisions

### Why Kafka over Redis pub/sub for all events?
Redis pub/sub is fire-and-forget — if a consumer is down when a message is published, the message is lost. For production incidents, losing an `anomaly.detected` event means an alert never fires. Kafka provides durability, consumer group rebalancing, and replay capability. Redis pub/sub is still used for the WebSocket relay layer where fire-and-forget is acceptable (the UI will catch up on next poll).

### Why Spring WebFlux for the BFF?
The BFF aggregates data from 4 services into one dashboard response. With Spring MVC, each parallel call blocks a thread while waiting for the response — at 4 services × 100ms average latency, that's 100ms wall time but 4 blocked threads per request. With WebFlux, all 4 calls are made simultaneously on a non-blocking event loop, reducing both wall time and thread consumption. The BFF is the correct place to absorb this complexity.

### Why Spring AI for direct LLM calls in Java?
Most Java-to-AI integrations simply proxy through a Python service for every AI call. The severity classification in the Incident Service is latency-sensitive (it blocks the incident creation response). Rather than adding a network hop to the Python service, Spring AI lets us call Claude directly from Java for this specific use case. The Python services handle the more complex, async AI pipelines (narration, RAG) where the extra network hop is acceptable.

### Why Resilience4j circuit breakers on the AI service?
LLM APIs have higher latency and lower reliability than typical internal services. Without a circuit breaker, a degraded AI service would cascade into the BFF timing out on dashboard loads. The circuit breaker opens after 5 failures in 10 seconds and serves cached narrations as fallback, keeping the dashboard functional during AI service outages.

### Why Azure Container Apps over AKS?
AKS is Kubernetes — appropriate at scale but adds significant operational overhead (control plane management, node pool sizing, networking complexity). For this project, Container Apps provides autoscaling, managed ingress, and per-service deployment without managing a Kubernetes cluster. If this were a product serving millions of traces per day, AKS would be the right answer.

### Why database-per-service?
Each microservice owns its schema and cannot directly query another service's database. This enforces service boundaries — cross-service data access must go through APIs. It also allows independent scaling of databases (the trace DB may need more IOPS than the auth DB) and independent schema migrations.

---

## 📁 Project structure

```
incident-commander-ai/
├── docker-compose.yml              # Local dev: all services + infra
├── docker-compose.prod.yml         # Azure-targeted compose
├── .env.example                    # Required env vars template
├── seed-demo.sh                    # Demo data seeder (20 mixed traces)
├── DEMO.md                         # Manual cURL commands for demo
│
├── bff-service/                    # Spring WebFlux API Gateway
│   ├── src/main/java/
│   │   └── com/incidentcommander/bff/
│   │       ├── controller/         # REST + WebSocket handlers
│   │       ├── aggregator/         # DashboardAggregator
│   │       ├── client/             # Typed WebClients per service
│   │       ├── resilience/         # Circuit breakers, fallbacks
│   │       └── relay/              # Redis → WebSocket fan-out
│   └── Dockerfile
│
├── trace-ingestion-service/        # Spring Boot
│   ├── src/main/java/
│   │   └── com/incidentcommander/trace/
│   │       ├── controller/
│   │       ├── service/            # SpanTreeWalker, DurationCalculator
│   │       ├── publisher/          # Kafka TraceEventPublisher
│   │       └── repository/
│   └── Dockerfile
│
├── incident-service/               # Spring Boot + Spring AI ★
│   ├── src/main/java/
│   │   └── com/incidentcommander/incident/
│   │       ├── controller/
│   │       ├── service/
│   │       │   ├── IncidentLifecycleService.java
│   │       │   ├── SimilarIncidentFinder.java   # Spring AI embeddings
│   │       │   └── SeverityClassifier.java      # Spring AI ChatClient
│   │       └── repository/
│   └── Dockerfile
│
├── alert-service/                  # Spring Boot
│   ├── src/main/java/
│   │   └── com/incidentcommander/alert/
│   │       ├── consumer/           # AnomalyEventConsumer (Kafka)
│   │       ├── evaluator/          # AlertEvaluator, RulesEngine
│   │       ├── controller/
│   │       └── repository/
│   └── Dockerfile
│
├── auth-service/                   # Spring Boot + Spring Security
│   └── Dockerfile
│
├── ai-narration-service/           # FastAPI (Python)
│   ├── app/
│   │   ├── main.py
│   │   ├── prompt_builder.py       # Span tree → LLM prompt
│   │   ├── llm_client.py           # Claude API client
│   │   ├── trace_sub.py            # Kafka subscriber (async)
│   │   └── models.py               # Pydantic schemas
│   └── Dockerfile
│
├── anomaly-detection-service/      # FastAPI (Python)
│   ├── app/
│   │   ├── main.py
│   │   ├── baseline_calculator.py
│   │   ├── z_score_detector.py
│   │   └── models.py
│   └── Dockerfile
│
├── rag-service/                    # FastAPI (Python)
│   ├── app/
│   │   ├── main.py
│   │   ├── embedding_generator.py
│   │   ├── vector_search.py
│   │   └── postmortem_writer.py
│   └── Dockerfile
│
├── frontend/                       # React TypeScript SPA
│   ├── src/
│   │   ├── pages/
│   │   │   ├── Dashboard.tsx
│   │   │   ├── TraceDetail.tsx
│   │   │   └── AlertCenter.tsx
│   │   ├── components/
│   │   │   ├── DAGGraph.tsx         # React Flow service topology
│   │   │   ├── SpanWaterfall.tsx    # Recharts horizontal bar
│   │   │   ├── NarrationPanel.tsx   # AI output with typewriter effect
│   │   │   ├── AlertBanner.tsx      # CRITICAL alert banner
│   │   │   └── MetricsCards.tsx
│   │   ├── store/                   # Zustand state
│   │   ├── hooks/
│   │   │   └── useWebSocket.ts      # WS hook → Zustand
│   │   └── api.ts                   # Axios client
│   └── Dockerfile
│
├── infrastructure/
│   ├── nginx.conf
│   ├── azure/
│   │   ├── container-apps.bicep     # Azure infra as code
│   │   └── app-insights.bicep
│   └── .github/
│       └── workflows/
│           └── deploy.yml           # GitHub Actions CI/CD
│
└── db/
    ├── traces-init.sql
    ├── incidents-init.sql
    ├── alerts-init.sql
    └── ai-init.sql                  # pgvector extension + schema
```

---

## 🚀 Getting started

### Prerequisites

- Docker and Docker Compose
- Java 17+
- Python 3.11+
- Node.js 20+
- An Anthropic API key (for Claude)

### 1. Clone the repository

```bash
git clone https://github.com/your-username/incident-commander-ai.git
cd incident-commander-ai
```

### 2. Configure environment

```bash
cp .env.example .env
```

Edit `.env` and fill in:

```env
# LLM
ANTHROPIC_API_KEY=sk-ant-...

# Databases (auto-created by Docker)
TRACES_DB_URL=postgresql://postgres:postgres@traces-db:5432/traces_db
INCIDENTS_DB_URL=postgresql://postgres:postgres@incidents-db:5432/incidents_db
ALERTS_DB_URL=postgresql://postgres:postgres@alerts-db:5432/alerts_db
AI_DB_URL=postgresql://postgres:postgres@ai-db:5432/ai_db

# Redis
REDIS_URL=redis://redis:6379

# Kafka
KAFKA_BOOTSTRAP_SERVERS=kafka:9092

# JWT
JWT_SECRET=your-secret-here-min-32-chars
```

### 3. Start all services

```bash
docker compose up --build
```

This starts:
- 4 PostgreSQL containers
- Redis
- Apache Kafka + Zookeeper
- All 7 microservices
- Nginx reverse proxy
- React frontend (dev server)

Wait for all services to report healthy (~60 seconds on first run).

### 4. Verify services are up

```bash
# BFF health
curl http://localhost:8080/actuator/health

# AI narration health
curl http://localhost:8081/health

# Anomaly detection health
curl http://localhost:8082/health
```

### 5. Seed demo data

```bash
chmod +x seed-demo.sh
./seed-demo.sh
```

This fires 20 traces — a mix of successful requests, slow requests, and cascading failures — and triggers the full AI analysis pipeline.

### 6. Open the dashboard

Navigate to **http://localhost:3000**

You should see:
- DAG graph with service nodes (some red/amber from the seeded failures)
- Traces appearing in the trace list
- AI narrations generating for failed traces
- Alerts firing for anomalous services

---

## 📨 Kafka event contracts

All services communicate through these topics. Both teams agreed on these schemas before building.

### `trace.ingested`
**Producer:** `trace-ingestion-service` (Java)
**Consumer:** `ai-narration-service` (Python)
**Partition key:** `service_name`

```json
{
  "trace_id": "abc-123",
  "root_service": "payment-service",
  "status": "ERROR",
  "duration_ms": 2340,
  "started_at": "2024-01-15T10:30:00Z",
  "spans": [
    {
      "span_id": "span-1",
      "parent_span_id": null,
      "operation": "POST /checkout",
      "service": "payment-service",
      "duration_ms": 2340,
      "status": "ERROR",
      "error_message": "Connection timeout to inventory-service"
    }
  ]
}
```

### `anomaly.detected`
**Producer:** `anomaly-detection-service` (Python)
**Consumer:** `alert-service` (Java)
**Partition key:** `service_name`

```json
{
  "service": "payment-service",
  "metric": "error_rate",
  "value": 0.34,
  "baseline_mean": 0.02,
  "z_score": 4.7,
  "severity": "CRITICAL",
  "detected_at": "2024-01-15T10:31:00Z"
}
```

### `narration.ready`
**Producer:** `ai-narration-service` (Python)
**Consumer:** `bff-service` (Java) → WebSocket
**Partition key:** `trace_id`

```json
{
  "trace_id": "abc-123",
  "root_cause": "The payment-service timed out waiting for inventory-service to respond...",
  "narrative": "At 10:30 UTC, a checkout request failed after 2.3 seconds...",
  "recommendations": [
    "Add a circuit breaker on the inventory-service client in payment-service",
    "Reduce connection timeout from 5s to 1s for non-critical downstream calls"
  ],
  "impacted_services": ["payment-service", "inventory-service"],
  "confidence": 0.87
}
```

### `alert.fired`
**Producer:** `alert-service` (Java)
**Consumer:** `bff-service` (Java) → WebSocket
**Partition key:** `severity`

```json
{
  "alert_id": "alert-456",
  "rule_name": "high-error-rate",
  "severity": "CRITICAL",
  "service": "payment-service",
  "message": "Error rate 34% exceeds threshold 10%",
  "fired_at": "2024-01-15T10:31:05Z"
}
```

---

## 📡 API reference

### BFF (port 8080)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/dashboard/overview` | Aggregated dashboard data (traces + alerts + metrics) |
| `GET` | `/api/traces` | Paginated trace list with filters |
| `GET` | `/api/traces/{id}` | Trace detail with spans + narration |
| `GET` | `/api/alerts` | Active alerts list |
| `PUT` | `/api/alerts/{id}/acknowledge` | Acknowledge an alert |
| `WS` | `/ws/live` | Real-time event stream (traces, narrations, alerts) |

### Trace ingestion (port 8081)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/traces` | Ingest a new trace with spans |
| `GET` | `/api/v1/traces/{id}` | Get trace by ID |
| `GET` | `/api/v1/services` | List all discovered services |
| `GET` | `/api/v1/services/{name}/dependencies` | Get service dependency graph data |

### AI narration (port 8082 — Python)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/narrate` | Trigger narration for a trace |
| `GET` | `/api/v1/narration/{trace_id}` | Get cached narration |

### Anomaly detection (port 8083 — Python)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/metrics` | Ingest a metric sample |
| `GET` | `/api/v1/anomalies` | List detected anomalies |
| `GET` | `/api/v1/baselines/{service}` | View current baselines for a service |

### Alert service (port 8084)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/alerts` | List alerts (filter by status, severity) |
| `POST` | `/api/v1/alerts` | Create manual alert |
| `PUT` | `/api/v1/alerts/{id}/acknowledge` | Acknowledge |
| `PUT` | `/api/v1/alerts/{id}/resolve` | Resolve |
| `GET` | `/api/v1/rules` | List alert rules |
| `POST` | `/api/v1/rules` | Create custom rule |

---

## ☁️ Azure deployment

The project is deployed on **Azure Container Apps** with a full CI/CD pipeline.

**Live URL:** https://incident-commander.azurecontainerapps.io *(replace with your actual URL)*

### Infrastructure

| Resource | Azure service |
|---|---|
| Application hosting | Azure Container Apps |
| Container registry | Azure Container Registry |
| PostgreSQL databases | Azure Database for PostgreSQL Flexible Server |
| Redis | Azure Cache for Redis |
| Kafka | Azure Event Hubs (Kafka-compatible API) |
| Observability | Azure Application Insights |
| CI/CD | GitHub Actions |

### CI/CD pipeline

Every push to `main` triggers:

```
push to main
    │
    ▼
GitHub Actions
    │
    ├── Build Docker images (parallel, one per service)
    ├── Run tests
    ├── Push to Azure Container Registry
    └── Deploy to Azure Container Apps
```

### What's monitored in App Insights

- LLM call latency (custom metric: `ai.narration.latency_ms`)
- Token usage and estimated cost per request (`ai.tokens.prompt`, `ai.tokens.completion`)
- Circuit breaker state changes (`resilience.circuit_breaker.state`)
- Kafka consumer lag per topic
- p99 end-to-end trace ingestion to narration latency

---

## 🎬 Demo

### Automated demo

```bash
./seed-demo.sh
```

Fires 20 traces including a simulated cascading failure in the payment service. Watch the UI:

1. DAG graph — payment-service node turns red
2. Trace list — failed traces appear with error badge
3. AI narration — root cause generates within ~3 seconds
4. Alert banner — CRITICAL alert appears at top of dashboard
5. Alert center — alert is listed, can be acknowledged

### Manual demo (if seed script fails)

See [DEMO.md](./DEMO.md) for step-by-step cURL commands.

### Quick manual trace injection

```bash
# Inject a failing trace
curl -X POST http://localhost:8080/api/v1/traces \
  -H "Content-Type: application/json" \
  -d '{
    "trace_id": "demo-001",
    "root_service": "payment-service",
    "status": "ERROR",
    "duration_ms": 3200,
    "started_at": "2024-01-15T10:30:00Z",
    "spans": [
      {
        "span_id": "s1",
        "parent_span_id": null,
        "operation": "POST /checkout",
        "service": "payment-service",
        "duration_ms": 3200,
        "status": "ERROR",
        "error_message": "Upstream timeout: inventory-service did not respond within 3000ms"
      },
      {
        "span_id": "s2",
        "parent_span_id": "s1",
        "operation": "GET /inventory/check",
        "service": "inventory-service",
        "duration_ms": 3000,
        "status": "TIMEOUT",
        "error_message": "Connection timeout"
      }
    ]
  }'
```

---

## 🤔 What I would do differently

**Idempotent Kafka consumers** — The current consumers don't guard against duplicate processing if a consumer restarts mid-message. Adding idempotency keys and a deduplication table would make the system truly exactly-once.

**Proper secrets management** — Environment variables work for development but in production, secrets should be in Azure Key Vault with managed identity access rather than Container Apps secrets.

**Async trace ingestion** — Currently the `POST /traces` endpoint is synchronous — it writes to the DB and then publishes to Kafka in the same request. At high volume, this would be a bottleneck. The write-ahead log pattern or an async queue would decouple ingestion latency from database write latency.

**LLM cost guardrails** — There's no rate limiting on AI narration generation. In production, every error trace would trigger a Claude API call. A budget cap, sampling strategy (only narrate traces above a severity threshold), or batching strategy would be necessary.

**Frontend E2E tests** — The frontend has no automated tests. Adding Playwright E2E tests for the critical flows (trace ingestion → narration → alert) would be the right next step.

---

## 👥 Authors

| Name | Role | GitHub |
|---|---|---|
| Harshit Chaudhary | Java Backend · Spring WebFlux · Spring AI · Azure | [@codeChaudhary](https://github.com/codechaudhary) |
| Abhishke Wani | Python Backend · FastAPI · AI Agents · RAG | [@friend-username](https://github.com/friend-username) |

---