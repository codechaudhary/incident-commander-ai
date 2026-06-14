# FEATURE INVENTORY

This document provides a strict, evidence-based inventory of all features described in the project. Features are classified based on their actual implementation status in the codebase.

## Implemented Features

### 1. Trace Ingestion & Span Parsing
* **Status:** Implemented
* **Evidence:** `trace-service` contains `TraceController.java` (`POST /api/v1/traces`), `SpanTreeWalker.java`, and `TraceIngestionService.java`.
* **Details:** Traces are ingested via REST, saved to a PostgreSQL database via `TraceRepository`, and emitted as `trace.ingested` Kafka events via `KafkaTraceEventProducer`.

### 2. Event-Driven Alerting (Rules-based)
* **Status:** Implemented (but deviates from README design)
* **Evidence:** `alert-service` contains `TraceEventConsumer.java` (`@KafkaListener`).
* **Details:** The Alert Service listens *directly* to the `trace.ingested` topic. If the trace status is `ERROR` or `TIMEOUT`, it calls `AlertCreationService.createAlertFromTrace`. The `anomaly.detected` flow described in the README is **not implemented**.

### 3. AI Root Cause Analysis (Narration)
* **Status:** Implemented
* **Evidence:** `ai-analytics-service` contains FastAPI routes (`/analyses/trigger`), an `aiokafka` consumer (`consumer.py`), and a prompt generator (`root_cause_prompt.py`).
* **Details:** The service consumes `trace.ingested`, calls an external LLM using `httpx` to analyze the spans, caches the response in Redis, and pushes it forward.

### 4. Real-time Dashboard via WebSockets
* **Status:** Implemented
* **Evidence:** `bff-service` contains `WebSocketConfig.java`, `DashboardController.java`, and `RedisSubscriberConfig.java`. `frontend` uses `STOMP.js` and `Zustand`.
* **Details:** The BFF acts as an API gateway using Spring WebFlux. It subscribes to Redis channels and relays events (like `alert.fired`) down to the frontend via WebSockets.

### 5. Synthetic Load Generation (Order Simulator)
* **Status:** Implemented
* **Evidence:** `order-simulator` service exists with a `SimulateController.java` and `TraceClient.java` capable of generating fake traffic and injecting it into the `trace-service`.

---

## Planned / Missing Features (Red Flags)

> [!WARNING]
> The following features are explicitly described in the `README.md` architecture but **cannot be found anywhere in the codebase**. They are classified as missing.

### 1. Anomaly Detection Service (Statistical Z-Score Analysis)
* **Status:** Missing (DESIGN INTENT DETECTED BUT IMPLEMENTATION NOT FOUND)
* **Evidence:** The README claims there is a Python service (`anomaly-detection-service`) calculating 1-hour baselines and emitting `anomaly.detected` Kafka events. 
* **Reality:** The codebase contains no such Python service, and `grep` reveals no publisher or consumer of `anomaly.detected` in any service. Alerting is done purely by looking at ERROR flags on incoming traces.

### 2. RAG Service (Vector Search & Postmortems)
* **Status:** Missing (DESIGN INTENT DETECTED BUT IMPLEMENTATION NOT FOUND)
* **Evidence:** The README references a `rag-service` using `pgvector` for semantic similarity search over past incidents.
* **Reality:** There is no `rag-service` directory. There are no pgvector dependencies in `requirements.txt` (`SQLAlchemy` is present but no vector extensions). No vector embedding calls are present.

### 3. Incident Lifecycle Service (Spring AI Integration)
* **Status:** Missing (DESIGN INTENT DETECTED BUT IMPLEMENTATION NOT FOUND)
* **Evidence:** The README claims there is an `incident-service` written in Java using `Spring AI` for direct LLM severity classification.
* **Reality:** No `incident-service` directory exists. No `spring-ai` dependency exists in any `pom.xml`.

### 4. Authentication & Authorization
* **Status:** Missing (DESIGN INTENT DETECTED BUT IMPLEMENTATION NOT FOUND)
* **Evidence:** The README claims an `auth-service` manages JWT tokens via Spring Security.
* **Reality:** No `auth-service` exists. No `spring-boot-starter-security` or JWT libraries exist in any `pom.xml`. The BFF gateway proxies requests without token validation.

### 5. Resilience4j Circuit Breakers
* **Status:** Partially Implemented / Missing
* **Evidence:** The README claims `bff-service` uses Resilience4j to protect against LLM API outages.
* **Reality:** `grep` for `resilience4j` in `pom.xml` yields no results. The circuit breaker pattern is not enforced via standard libraries (requires further LLD inspection to see if it's custom implemented).
