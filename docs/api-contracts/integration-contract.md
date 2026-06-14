# AI-Assisted Incident Analysis Platform
## Integration Contracts — Frozen Before Development Starts

> **Version:** 1.0.0 | **Status:** FROZEN  
> **Scope:** Portfolio demo, 2-week sprint  
> **Last Updated:** 2025-06-06

---

## Table of Contents

1. [Service Boundaries & Responsibilities](#1-service-boundaries--responsibilities)
2. [Shared Enums & Status Values](#2-shared-enums--status-values)
3. [Error Response Contract](#3-error-response-contract)
4. [PostgreSQL Schema](#4-postgresql-schema)
5. [OpenTelemetry Trace Payload Model](#5-opentelemetry-trace-payload-model)
6. [Kafka Topic Definitions](#6-kafka-topic-definitions)
7. [Kafka Event Payload Contracts](#7-kafka-event-payload-contracts)
8. [Redis Channel Contracts](#8-redis-channel-contracts)
9. [REST API Contracts](#9-rest-api-contracts)
10. [WebSocket Message Contracts](#10-websocket-message-contracts)
11. [AI Service API Contract](#11-ai-service-api-contract)
12. [Frontend Pages & API Consumption](#12-frontend-pages--api-consumption)
13. [Folder Structure](#13-folder-structure)
14. [Sequence Diagrams](#14-sequence-diagrams)
15. [API Versioning Strategy](#15-api-versioning-strategy)
16. [Contract-First Development Recommendations](#16-contract-first-development-recommendations)
17. [Dependency & Boot Order](#17-dependency--boot-order)

---

## 1. Service Boundaries & Responsibilities

### 1.1 Service Map

| Service | Runtime | Port | Owner | Responsibility |
|---|---|---|---|---|
| `order-simulator` | Java Spring Boot | 8081 | Dev A | Simulates order→payment calls with injected failures |
| `trace-service` | Java Spring Boot | 8082 | Dev A | Receives OTel spans, persists traces + spans to PostgreSQL, publishes to Kafka |
| `alert-service` | Java Spring Boot | 8083 | Dev A | Consumes Kafka, creates alerts, publishes to Redis Pub/Sub |
| `bff-service` | Java Spring WebFlux | 8080 | Dev A | API gateway for frontend; aggregates trace+alert+AI data; proxies WebSocket |
| `ai-analysis-service` | Python FastAPI | 8090 | Dev B | Consumes Kafka, calls LLM, stores AI analysis result to PostgreSQL |
| `frontend` | React + TypeScript | 3000 | Dev A/B | Dashboard UI |

### 1.2 Responsibility Boundaries (Non-Negotiable)

```
order-simulator      →  only generates OTel spans; no business logic
trace-service        →  only WRITE path for traces; no alerting logic
alert-service        →  only alert lifecycle; reads from Kafka; no trace writes
bff-service          →  only READ aggregation + WebSocket relay; no business logic
ai-analysis-service  →  only LLM calls; reads from Kafka; writes ai_analysis to DB
frontend             →  only consumes BFF; never calls trace/alert/AI services directly
```

---

## 2. Shared Enums & Status Values

> **Ownership:** Defined here. Dev A mirrors in Java (`enums` package). Dev B mirrors in Python (`models/enums.py`). Frontend mirrors in TypeScript (`src/types/enums.ts`).

### 2.1 TraceStatus

```typescript
enum TraceStatus {
  SUCCESS = "SUCCESS",
  ERROR   = "ERROR",
  TIMEOUT = "TIMEOUT"
}
```

### 2.2 SpanStatus

```typescript
enum SpanStatus {
  OK    = "OK",
  ERROR = "ERROR"
}
```

### 2.3 AlertSeverity

```typescript
enum AlertSeverity {
  LOW      = "LOW",
  MEDIUM   = "MEDIUM",
  HIGH     = "HIGH",
  CRITICAL = "CRITICAL"
}
```

### 2.4 AlertStatus

```typescript
enum AlertStatus {
  OPEN        = "OPEN",
  ACKNOWLEDGED = "ACKNOWLEDGED",
  RESOLVED    = "RESOLVED"
}
```

### 2.5 AnalysisStatus

```typescript
enum AnalysisStatus {
  PENDING    = "PENDING",
  PROCESSING = "PROCESSING",
  COMPLETED  = "COMPLETED",
  FAILED     = "FAILED"
}
```

### 2.6 FailureType (Injected by order-simulator)

```typescript
enum FailureType {
  NONE              = "NONE",
  SLOW_RESPONSE     = "SLOW_RESPONSE",
  DB_TIMEOUT        = "DB_TIMEOUT",
  RUNTIME_EXCEPTION = "RUNTIME_EXCEPTION"
}
```

---

## 3. Error Response Contract

**Purpose:** Uniform error shape across all services.  
**Ownership:** Each service implements; BFF normalizes before forwarding to frontend.

### 3.1 Schema

```json
{
  "timestamp":  "string (ISO-8601)",
  "status":     "number (HTTP status code)",
  "error":      "string (HTTP status text)",
  "message":    "string (human-readable detail)",
  "path":       "string (request URI)",
  "traceId":    "string | null (correlation ID if available)"
}
```

### 3.2 Example

```json
{
  "timestamp": "2025-06-06T10:23:45.123Z",
  "status": 404,
  "error": "Not Found",
  "message": "Trace with id 'abc-123' not found",
  "path": "/api/v1/traces/abc-123",
  "traceId": null
}
```

### 3.3 Validation Rules

- `timestamp` — always present, always UTC ISO-8601
- `status` — matches HTTP response status code
- `traceId` — present when the request carries an OTel trace context header
- Services MUST NOT return raw stack traces in `message` in any environment

---

## 4. PostgreSQL Schema

**Ownership:** Dev A creates migrations. AI service (Dev B) reads `ai_analysis` table.

### 4.1 `traces`

```sql
CREATE TABLE traces (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trace_id      VARCHAR(64)  NOT NULL UNIQUE,  -- OTel trace ID (hex)
    root_service  VARCHAR(128) NOT NULL,          -- e.g. "order-service"
    root_operation VARCHAR(128) NOT NULL,         -- e.g. "POST /orders"
    status        VARCHAR(16)  NOT NULL,          -- TraceStatus enum
    failure_type  VARCHAR(32)  NOT NULL DEFAULT 'NONE',
    duration_ms   BIGINT       NOT NULL,
    started_at    TIMESTAMPTZ  NOT NULL,
    ended_at      TIMESTAMPTZ  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_traces_trace_id  ON traces(trace_id);
CREATE INDEX idx_traces_status    ON traces(status);
CREATE INDEX idx_traces_started_at ON traces(started_at DESC);
```

### 4.2 `spans`

```sql
CREATE TABLE spans (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trace_id     VARCHAR(64)  NOT NULL REFERENCES traces(trace_id) ON DELETE CASCADE,
    span_id      VARCHAR(32)  NOT NULL,
    parent_span_id VARCHAR(32),
    service_name VARCHAR(128) NOT NULL,
    operation    VARCHAR(256) NOT NULL,
    status       VARCHAR(16)  NOT NULL,   -- SpanStatus enum
    duration_ms  BIGINT       NOT NULL,
    started_at   TIMESTAMPTZ  NOT NULL,
    ended_at     TIMESTAMPTZ  NOT NULL,
    attributes   JSONB        NOT NULL DEFAULT '{}',
    events       JSONB        NOT NULL DEFAULT '[]',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_spans_trace_id ON spans(trace_id);
CREATE INDEX idx_spans_span_id  ON spans(span_id);
```

### 4.3 `alerts`

```sql
CREATE TABLE alerts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    alert_id    VARCHAR(64)  NOT NULL UNIQUE,
    trace_id    VARCHAR(64)  NOT NULL,
    severity    VARCHAR(16)  NOT NULL,   -- AlertSeverity enum
    status      VARCHAR(24)  NOT NULL DEFAULT 'OPEN',  -- AlertStatus enum
    title       VARCHAR(256) NOT NULL,
    description TEXT         NOT NULL,
    triggered_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_alerts_trace_id    ON alerts(trace_id);
CREATE INDEX idx_alerts_status      ON alerts(status);
CREATE INDEX idx_alerts_triggered_at ON alerts(triggered_at DESC);
```

### 4.4 `ai_analysis`

```sql
CREATE TABLE ai_analysis (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    analysis_id     VARCHAR(64)  NOT NULL UNIQUE,
    trace_id        VARCHAR(64)  NOT NULL,
    alert_id        VARCHAR(64),
    status          VARCHAR(16)  NOT NULL DEFAULT 'PENDING',  -- AnalysisStatus enum
    root_cause      TEXT,
    affected_services TEXT[],
    recommendations TEXT[],
    confidence_score NUMERIC(4,3),   -- 0.000 to 1.000
    model_used      VARCHAR(128),
    prompt_tokens   INT,
    completion_tokens INT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMPTZ
);

CREATE INDEX idx_ai_analysis_trace_id ON ai_analysis(trace_id);
CREATE INDEX idx_ai_analysis_status   ON ai_analysis(status);
```

---

## 5. OpenTelemetry Trace Payload Model

**Purpose:** Defines the OTel span structure that `order-simulator` generates and `trace-service` receives via the OTel HTTP/gRPC exporter endpoint.  
**Ownership:** Dev A (both producer and receiver).  
**Protocol:** OTel Collector OTLP/HTTP JSON format (simplified).

### 5.1 Schema

```json
{
  "resourceSpans": [
    {
      "resource": {
        "attributes": [
          { "key": "service.name",    "value": { "stringValue": "string" } },
          { "key": "service.version", "value": { "stringValue": "string" } }
        ]
      },
      "scopeSpans": [
        {
          "scope": { "name": "string", "version": "string" },
          "spans": [
            {
              "traceId":       "string (hex, 32 chars)",
              "spanId":        "string (hex, 16 chars)",
              "parentSpanId":  "string (hex, 16 chars) | null",
              "name":          "string",
              "kind":          "number (SpanKind: 1=INTERNAL,2=SERVER,3=CLIENT)",
              "startTimeUnixNano": "string (nanoseconds epoch)",
              "endTimeUnixNano":   "string (nanoseconds epoch)",
              "status": {
                "code":    "number (0=UNSET,1=OK,2=ERROR)",
                "message": "string"
              },
              "attributes": [
                { "key": "string", "value": { "stringValue": "string" } }
              ],
              "events": [
                {
                  "timeUnixNano": "string",
                  "name":         "string",
                  "attributes":   []
                }
              ]
            }
          ]
        }
      ]
    }
  ]
}
```

### 5.2 Required Span Attributes (Injected by order-simulator)

| Attribute Key | Type | Example |
|---|---|---|
| `http.method` | string | `"POST"` |
| `http.url` | string | `"http://payment-service/charge"` |
| `http.status_code` | int | `500` |
| `failure.type` | string | `"DB_TIMEOUT"` |
| `failure.injected` | bool | `true` |
| `db.system` | string | `"postgresql"` (on DB spans) |

### 5.3 Example Trace (Failure Scenario)

```json
{
  "resourceSpans": [{
    "resource": {
      "attributes": [
        { "key": "service.name", "value": { "stringValue": "order-service" } }
      ]
    },
    "scopeSpans": [{
      "scope": { "name": "order-simulator", "version": "1.0.0" },
      "spans": [
        {
          "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
          "spanId": "00f067aa0ba902b7",
          "parentSpanId": null,
          "name": "POST /orders",
          "kind": 2,
          "startTimeUnixNano": "1717660800000000000",
          "endTimeUnixNano":   "1717660805200000000",
          "status": { "code": 2, "message": "payment failed" },
          "attributes": [
            { "key": "http.method",      "value": { "stringValue": "POST" } },
            { "key": "http.status_code", "value": { "intValue": 500 } },
            { "key": "failure.type",     "value": { "stringValue": "DB_TIMEOUT" } },
            { "key": "failure.injected", "value": { "boolValue": true } }
          ],
          "events": [
            {
              "timeUnixNano": "1717660805100000000",
              "name": "exception",
              "attributes": [
                { "key": "exception.message", "value": { "stringValue": "DB timeout after 5000ms" } }
              ]
            }
          ]
        },
        {
          "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
          "spanId": "a2fb4a1d1a96d312",
          "parentSpanId": "00f067aa0ba902b7",
          "name": "POST /charge",
          "kind": 3,
          "startTimeUnixNano": "1717660800050000000",
          "endTimeUnixNano":   "1717660805150000000",
          "status": { "code": 2, "message": "RuntimeException: DB timeout" },
          "attributes": [
            { "key": "http.method",      "value": { "stringValue": "POST" } },
            { "key": "http.url",         "value": { "stringValue": "http://payment-service/charge" } },
            { "key": "http.status_code", "value": { "intValue": 500 } },
            { "key": "failure.type",     "value": { "stringValue": "DB_TIMEOUT" } }
          ],
          "events": []
        }
      ]
    }]
  }]
}
```

### 5.4 Validation Rules

- `traceId` — 32 hex chars, non-null, non-empty
- `spanId` — 16 hex chars
- `parentSpanId` — null for root span only; must reference a valid span in same trace
- `startTimeUnixNano` < `endTimeUnixNano`
- At least 1 span per request; at least 1 span must have `parentSpanId = null` (root)
- `failure.type` must match the `FailureType` enum

---

## 6. Kafka Topic Definitions

**Ownership:** Dev A creates topics (docker-compose).

| Topic | Partitions | Replication | Retention | Producers | Consumers |
|---|---|---|---|---|---|
| `trace-events` | 3 | 1 | 24h | trace-service | alert-service, ai-analysis-service |
| `alert-events` | 1 | 1 | 24h | alert-service | (future / debug) |

### 6.1 Kafka Configuration (docker-compose snippet)

```yaml
kafka:
  environment:
    KAFKA_CREATE_TOPICS: "trace-events:3:1,alert-events:1:1"
    KAFKA_AUTO_CREATE_TOPICS_ENABLE: "false"
    KAFKA_LOG_RETENTION_HOURS: "24"
```

### 6.2 Consumer Group IDs

| Consumer Group | Service |
|---|---|
| `alert-service-group` | alert-service |
| `ai-analysis-group` | ai-analysis-service |

---

## 7. Kafka Event Payload Contracts

All Kafka messages use **JSON serialization** with a **header envelope**.

### 7.1 Message Envelope (all topics)

```json
{
  "eventId":      "string (UUID v4)",
  "eventType":    "string (see per-topic)",
  "eventVersion": "string (semver, e.g. '1.0.0')",
  "timestamp":    "string (ISO-8601 UTC)",
  "source":       "string (service name)",
  "payload":      "object (event-specific)"
}
```

---

### 7.2 Topic: `trace-events`

**Event Type:** `TRACE_INGESTED`

**Purpose:** Notifies downstream services that a trace has been fully ingested and persisted.  
**Producer:** trace-service  
**Consumers:** alert-service, ai-analysis-service  
**Ownership:** Dev A (schema); Dev A (producer); Dev A + Dev B (consumers)

#### Schema

```json
{
  "eventId":      "string (UUID)",
  "eventType":    "TRACE_INGESTED",
  "eventVersion": "1.0.0",
  "timestamp":    "string (ISO-8601)",
  "source":       "trace-service",
  "payload": {
    "traceId":      "string",
    "rootService":  "string",
    "rootOperation":"string",
    "status":       "string (TraceStatus)",
    "failureType":  "string (FailureType)",
    "durationMs":   "number",
    "startedAt":    "string (ISO-8601)",
    "endedAt":      "string (ISO-8601)",
    "spanCount":    "number",
    "errorSpans":   [
      {
        "spanId":      "string",
        "serviceName": "string",
        "operation":   "string",
        "errorMessage":"string",
        "durationMs":  "number"
      }
    ]
  }
}
```

#### Example Payload

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "eventType": "TRACE_INGESTED",
  "eventVersion": "1.0.0",
  "timestamp": "2025-06-06T10:30:00.000Z",
  "source": "trace-service",
  "payload": {
    "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
    "rootService": "order-service",
    "rootOperation": "POST /orders",
    "status": "ERROR",
    "failureType": "DB_TIMEOUT",
    "durationMs": 5200,
    "startedAt": "2025-06-06T10:30:00.000Z",
    "endedAt": "2025-06-06T10:30:05.200Z",
    "spanCount": 4,
    "errorSpans": [
      {
        "spanId": "a2fb4a1d1a96d312",
        "serviceName": "payment-service",
        "operation": "POST /charge",
        "errorMessage": "RuntimeException: DB timeout after 5000ms",
        "durationMs": 5100
      }
    ]
  }
}
```

#### Validation Rules

- `traceId` — non-null, matches OTel trace ID stored in DB
- `status` — must be valid `TraceStatus` enum value
- `failureType` — must be valid `FailureType` enum value
- `durationMs` — positive integer
- `errorSpans` — empty array `[]` when `status == SUCCESS`; non-empty when `status == ERROR` or `TIMEOUT`
- `spanCount` — minimum 1

#### Kafka Message Key

```
Key: {traceId}
```
(Ensures all events for same trace go to same partition, preserving order.)

---

### 7.3 Topic: `alert-events`

**Event Type:** `ALERT_CREATED`

**Purpose:** Internal observability / future consumer extensibility.  
**Producer:** alert-service  
**Consumers:** None required for demo (optional debug consumer)  
**Ownership:** Dev A

#### Schema

```json
{
  "eventId":      "string (UUID)",
  "eventType":    "ALERT_CREATED",
  "eventVersion": "1.0.0",
  "timestamp":    "string (ISO-8601)",
  "source":       "alert-service",
  "payload": {
    "alertId":   "string",
    "traceId":   "string",
    "severity":  "string (AlertSeverity)",
    "status":    "string (AlertStatus)",
    "title":     "string",
    "description": "string",
    "triggeredAt": "string (ISO-8601)"
  }
}
```

---

## 8. Redis Channel Contracts

**Purpose:** alert-service publishes real-time alert notifications; bff-service subscribes and pushes to WebSocket clients.  
**Ownership:** Dev A (both publisher and subscriber).

### 8.1 Channel Naming

| Channel | Publisher | Subscriber |
|---|---|---|
| `alerts:live` | alert-service | bff-service |
| `analysis:live` | ai-analysis-service | bff-service |

### 8.2 `alerts:live` Message Schema

```json
{
  "type":      "ALERT_CREATED",
  "alertId":   "string",
  "traceId":   "string",
  "severity":  "string (AlertSeverity)",
  "status":    "string (AlertStatus)",
  "title":     "string",
  "description": "string",
  "triggeredAt": "string (ISO-8601)"
}
```

### 8.3 `analysis:live` Message Schema

```json
{
  "type":          "ANALYSIS_COMPLETED",
  "analysisId":    "string",
  "traceId":       "string",
  "alertId":       "string | null",
  "status":        "string (AnalysisStatus)",
  "rootCause":     "string",
  "recommendations": ["string"],
  "confidenceScore": "number (0.0-1.0)",
  "completedAt":   "string (ISO-8601)"
}
```

### 8.4 Validation Rules

- Messages are JSON strings (not binary)
- Subscribers must handle parse failures gracefully (log + skip, do not crash)
- No TTL on Pub/Sub messages (fire-and-forget)
- bff-service must not block on Redis subscribe; use reactive/async subscribe

---

## 9. REST API Contracts

**Base URL pattern:** `http://{host}:{port}/api/v1/`  
**Content-Type:** `application/json`  
**Auth:** None (out of scope)

---

### 9.1 Trace Service API

**Base:** `http://trace-service:8082/api/v1`  
**Ownership:** Dev A

#### POST `/traces` — Ingest OTel Trace

**Purpose:** Receives OTLP/HTTP JSON payload from OTel SDK.  
**Note:** This is the OTel Collector receiver endpoint format. In production you'd use a real OTel Collector; here trace-service acts as one.

Request: OTel OTLP/HTTP JSON (see Section 5)  
Response `201 Created`:

```json
{
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "status":  "INGESTED",
  "spanCount": 4
}
```

Response `400 Bad Request`: See Error Response Contract (Section 3)

---

#### GET `/traces` — List Traces

Query params:

| Param | Type | Default | Description |
|---|---|---|---|
| `page` | int | 0 | 0-indexed |
| `size` | int | 20 | Max 100 |
| `status` | string | null | Filter by TraceStatus |
| `from` | ISO-8601 | null | Start time filter |
| `to` | ISO-8601 | null | End time filter |

Response `200 OK`:

```json
{
  "content": [
    {
      "id":            "uuid",
      "traceId":       "string",
      "rootService":   "string",
      "rootOperation": "string",
      "status":        "string (TraceStatus)",
      "failureType":   "string (FailureType)",
      "durationMs":    5200,
      "startedAt":     "string (ISO-8601)",
      "endedAt":       "string (ISO-8601)",
      "spanCount":     4,
      "createdAt":     "string (ISO-8601)"
    }
  ],
  "page":          0,
  "size":          20,
  "totalElements": 145,
  "totalPages":    8
}
```

---

#### GET `/traces/{traceId}` — Get Trace Detail

Response `200 OK`:

```json
{
  "id":            "uuid",
  "traceId":       "4bf92f3577b34da6a3ce929d0e0e4736",
  "rootService":   "order-service",
  "rootOperation": "POST /orders",
  "status":        "ERROR",
  "failureType":   "DB_TIMEOUT",
  "durationMs":    5200,
  "startedAt":     "2025-06-06T10:30:00.000Z",
  "endedAt":       "2025-06-06T10:30:05.200Z",
  "createdAt":     "2025-06-06T10:30:05.500Z",
  "spans": [
    {
      "spanId":        "00f067aa0ba902b7",
      "parentSpanId":  null,
      "serviceName":   "order-service",
      "operation":     "POST /orders",
      "status":        "ERROR",
      "durationMs":    5200,
      "startedAt":     "2025-06-06T10:30:00.000Z",
      "endedAt":       "2025-06-06T10:30:05.200Z",
      "attributes": {
        "http.method": "POST",
        "http.status_code": 500,
        "failure.type": "DB_TIMEOUT"
      },
      "events": [
        {
          "timeUnixNano": "1717660805100000000",
          "name": "exception",
          "attributes": { "exception.message": "DB timeout after 5000ms" }
        }
      ]
    }
  ]
}
```

Response `404 Not Found`: Error Response Contract

---

### 9.2 Alert Service API

**Base:** `http://alert-service:8083/api/v1`  
**Ownership:** Dev A

#### GET `/alerts` — List Alerts

Query params:

| Param | Type | Default | Description |
|---|---|---|---|
| `page` | int | 0 | 0-indexed |
| `size` | int | 20 | Max 100 |
| `status` | string | null | Filter by AlertStatus |
| `severity` | string | null | Filter by AlertSeverity |
| `traceId` | string | null | Filter by trace |

Response `200 OK`:

```json
{
  "content": [
    {
      "id":          "uuid",
      "alertId":     "string",
      "traceId":     "string",
      "severity":    "CRITICAL",
      "status":      "OPEN",
      "title":       "Payment Service DB Timeout",
      "description": "Payment service failed with DB timeout. Duration: 5200ms.",
      "triggeredAt": "2025-06-06T10:30:05.800Z",
      "updatedAt":   "2025-06-06T10:30:05.800Z"
    }
  ],
  "page":          0,
  "size":          20,
  "totalElements": 12,
  "totalPages":    1
}
```

---

#### GET `/alerts/{alertId}` — Get Alert Detail

Response `200 OK`: Single alert object (same shape as list item)  
Response `404`: Error Response Contract

---

#### PATCH `/alerts/{alertId}/status` — Update Alert Status

**Purpose:** Allows frontend to acknowledge or resolve an alert.

Request:

```json
{
  "status": "ACKNOWLEDGED"
}
```

Validation: `status` must be `ACKNOWLEDGED` or `RESOLVED` (cannot set back to `OPEN`)

Response `200 OK`: Updated alert object  
Response `400`: Error Response Contract

---

### 9.3 AI Analysis Service API

**Base:** `http://ai-analysis-service:8090/api/v1`  
**Ownership:** Dev B

#### GET `/analyses/{traceId}` — Get Analysis for Trace

Response `200 OK`:

```json
{
  "id":             "uuid",
  "analysisId":     "string",
  "traceId":        "string",
  "alertId":        "string | null",
  "status":         "COMPLETED",
  "rootCause":      "The payment-service experienced a database connection timeout after 5000ms, likely caused by connection pool exhaustion under load. The order-service propagated this failure upstream, resulting in a 500 error for the end user.",
  "affectedServices": ["order-service", "payment-service"],
  "recommendations": [
    "Increase DB connection pool size in payment-service",
    "Add circuit breaker between order-service and payment-service",
    "Implement retry with exponential backoff"
  ],
  "confidenceScore": 0.87,
  "modelUsed":      "gpt-4o-mini",
  "promptTokens":   412,
  "completionTokens": 198,
  "createdAt":      "2025-06-06T10:30:06.000Z",
  "completedAt":    "2025-06-06T10:30:08.300Z"
}
```

Response `202 Accepted` (analysis still pending):

```json
{
  "analysisId": "string",
  "traceId":    "string",
  "status":     "PROCESSING",
  "message":    "Analysis in progress"
}
```

Response `404`: Error Response Contract

---

#### POST `/analyses/trigger` — Manually Trigger Analysis

**Purpose:** Demo / testing endpoint to re-trigger AI analysis for a trace.  
**Ownership:** Dev B

Request:

```json
{
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736"
}
```

Response `202 Accepted`:

```json
{
  "analysisId": "uuid",
  "traceId":    "string",
  "status":     "PENDING",
  "message":    "Analysis queued"
}
```

---

### 9.4 BFF Service API

**Base:** `http://bff-service:8080/api/v1`  
**Ownership:** Dev A  
**Note:** BFF aggregates responses from downstream services. It does NOT call OTel or Kafka directly.

#### GET `/dashboard/summary` — Dashboard Summary Stats

Response `200 OK`:

```json
{
  "totalTraces":        145,
  "errorTraces":        12,
  "openAlerts":         5,
  "criticalAlerts":     2,
  "completedAnalyses":  10,
  "avgTraceDurationMs": 342,
  "lastUpdated":        "2025-06-06T10:30:00.000Z"
}
```

---

#### GET `/incidents/{traceId}` — Full Incident View (Aggregated)

**Purpose:** Single call from frontend to get trace + alert + AI analysis for one incident.

Response `200 OK`:

[//]: # (```json)

[//]: # ({)

[//]: # (  "trace": { /* GET /traces/{traceId} response shape */ },)

[//]: # (  "alert": { /* GET /alerts/{alertId} response shape, or null */ },)

[//]: # (  "analysis": { /* GET /analyses/{traceId} response shape, or null */ })

[//]: # (})

[//]: # (```)

Response `404`: Error Response Contract

---

#### GET `/traces` — Proxied Trace List

Proxies to trace-service. Same query params, same response shape.

#### GET `/alerts` — Proxied Alert List

Proxies to alert-service. Same query params, same response shape.

---

### 9.5 Order Simulator API

**Base:** `http://order-simulator:8081/api/v1`  
**Ownership:** Dev A

#### POST `/simulate` — Trigger Simulated Order

**Purpose:** Frontend "trigger" button calls this to start a demo scenario.

Request:

```json
{
  "failureType": "DB_TIMEOUT",
  "delayMs":     5000
}
```

| Field | Type | Required | Default |
|---|---|---|---|
| `failureType` | FailureType enum | No | `NONE` |
| `delayMs` | int (0–10000) | No | `0` |

Response `200 OK`:

```json
{
  "orderId":    "uuid",
  "traceId":    "4bf92f3577b34da6a3ce929d0e0e4736",
  "failureType":"DB_TIMEOUT",
  "status":     "SIMULATED",
  "message":    "Order simulation triggered. Trace will appear in dashboard within 2-3 seconds."
}
```

---

## 10. WebSocket Message Contracts

**Ownership:** Dev A (server-side); Dev A/B (frontend consumption)  
**Protocol:** STOMP over WebSocket  
**Endpoint:** `ws://bff-service:8080/ws`

### 10.1 STOMP Broker Topics (Subscribe)

| Topic | Description |
|---|---|
| `/topic/alerts` | Live alert notifications |
| `/topic/analysis` | Live AI analysis completions |
| `/topic/traces` | Live trace ingestion notifications |

### 10.2 WebSocket Alert Message

**Topic:** `/topic/alerts`

```json
{
  "type":        "ALERT_CREATED",
  "alertId":     "string",
  "traceId":     "string",
  "severity":    "CRITICAL",
  "status":      "OPEN",
  "title":       "Payment Service DB Timeout",
  "description": "string",
  "triggeredAt": "2025-06-06T10:30:05.800Z"
}
```

### 10.3 WebSocket Analysis Message

**Topic:** `/topic/analysis`

```json
{
  "type":            "ANALYSIS_COMPLETED",
  "analysisId":      "string",
  "traceId":         "string",
  "alertId":         "string | null",
  "status":          "COMPLETED",
  "rootCause":       "string",
  "recommendations": ["string"],
  "confidenceScore": 0.87,
  "completedAt":     "2025-06-06T10:30:08.300Z"
}
```

### 10.4 WebSocket Trace Notification Message

**Topic:** `/topic/traces`

```json
{
  "type":          "TRACE_INGESTED",
  "traceId":       "string",
  "rootService":   "string",
  "rootOperation": "string",
  "status":        "ERROR",
  "failureType":   "DB_TIMEOUT",
  "durationMs":    5200,
  "startedAt":     "2025-06-06T10:30:00.000Z"
}
```

### 10.5 Validation Rules

- Client must handle `STOMP ERROR` frames gracefully (reconnect with backoff)
- Messages are JSON strings in STOMP body
- bff-service relays Redis Pub/Sub messages → STOMP topics without transformation (same payload shape)
- Frontend must handle duplicate messages (idempotent by `alertId` / `analysisId`)

---

## 11. AI Service API Contract

**Ownership:** Dev B (implements); Dev A (consumes via BFF)

### 11.1 Internal Kafka Consumer Behavior

The AI service consumes `trace-events` topic.  
**Trigger condition:** `payload.status == "ERROR" OR payload.status == "TIMEOUT"`  
Successful traces (`status == "SUCCESS"`) are skipped.

### 11.2 LLM Prompt Contract

**Purpose:** Defines the prompt structure sent to the LLM. Frozen to prevent scope creep.

#### System Prompt (frozen)

```
You are an expert Site Reliability Engineer (SRE) analyzing distributed system traces.
You will be given a trace from an OpenTelemetry-instrumented system that has experienced an error or timeout.
Analyze the trace and provide a concise root cause analysis.

Respond ONLY in the following JSON format, with no additional text:
{
  "rootCause": "string (1-3 sentences explaining what failed and why)",
  "affectedServices": ["array of service names"],
  "recommendations": ["array of 2-4 actionable recommendations"],
  "confidenceScore": 0.0-1.0
}
```

#### User Prompt Template (frozen)

```
Trace ID: {traceId}
Root Service: {rootService}
Operation: {rootOperation}
Status: {status}
Total Duration: {durationMs}ms
Failure Type: {failureType}

Error Spans:
{errorSpansJson}

All Span Summary:
{spanSummaryJson}

Analyze this trace and identify the root cause.
```

### 11.3 LLM Configuration (frozen defaults)

| Parameter | Value |
|---|---|
| Model (OpenAI) | `gpt-4o-mini` |
| Model (Anthropic) | `claude-haiku-4-5-20251001` |
| Max tokens | 500 |
| Temperature | 0.2 |
| Timeout | 30s |

### 11.4 AI Service Environment Variables

```env
LLM_PROVIDER=openai          # "openai" or "anthropic"
OPENAI_API_KEY=...
ANTHROPIC_API_KEY=...
DATABASE_URL=postgresql://user:pass@postgres:5432/incidents
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
REDIS_URL=redis://redis:6379
```

### 11.5 AI Analysis Processing Flow

```
1. Consume TRACE_INGESTED from Kafka
2. If status != ERROR/TIMEOUT → skip
3. INSERT ai_analysis row with status=PENDING
4. Fetch full span data from PostgreSQL (via DB, not API)
5. Build prompt using template above
6. Call LLM API
7. Parse JSON response
8. UPDATE ai_analysis row with results, status=COMPLETED
9. Publish to Redis channel `analysis:live`
```

---

## 12. Frontend Pages & API Consumption

**Stack:** React 18, TypeScript, React Query (TanStack Query), STOMP.js, Tailwind CSS  
**Ownership:** Dev A/B (shared)

### 12.1 Page List

| Page | Route | Description |
|---|---|---|
| Dashboard | `/` | Summary stats + live alert/trace feed |
| Incidents List | `/incidents` | Paginated list of all traces with alert status |
| Incident Detail | `/incidents/:traceId` | Full trace + spans + alert + AI analysis |
| Simulate | `/simulate` | Trigger demo scenario with failure type selector |

### 12.2 Component → API Mapping

#### `/` Dashboard

| Component | API Call | Update Method |
|---|---|---|
| Summary Cards | `GET /api/v1/dashboard/summary` | Polling every 10s |
| Live Alert Feed | `GET /api/v1/alerts?status=OPEN&size=10` | WebSocket `/topic/alerts` |
| Recent Traces | `GET /api/v1/traces?size=10` | WebSocket `/topic/traces` |

#### `/incidents` Incidents List

| Component | API Call |
|---|---|
| Incidents Table | `GET /api/v1/traces?page=0&size=20` |
| Alert badges | Merged from `GET /api/v1/alerts` (client-side join by traceId) |

#### `/incidents/:traceId` Incident Detail

| Component | API Call |
|---|---|
| Full incident | `GET /api/v1/incidents/:traceId` (BFF aggregate) |
| Trace Waterfall | Rendered from `incident.trace.spans` |
| Alert Card | Rendered from `incident.alert` |
| AI Analysis Panel | Rendered from `incident.analysis`; polls if status=PROCESSING |
| Acknowledge Button | `PATCH /api/v1/alerts/:alertId/status` |

#### `/simulate` Simulate

| Component | API Call |
|---|---|
| Trigger Button | `POST /api/v1/simulate` |

### 12.3 TypeScript DTO Types (Frontend)

```typescript
// Shared enums — mirrors Section 2
export type TraceStatus   = "SUCCESS" | "ERROR" | "TIMEOUT";
export type SpanStatus    = "OK" | "ERROR";
export type AlertSeverity = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
export type AlertStatus   = "OPEN" | "ACKNOWLEDGED" | "RESOLVED";
export type AnalysisStatus = "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";
export type FailureType   = "NONE" | "SLOW_RESPONSE" | "DB_TIMEOUT" | "RUNTIME_EXCEPTION";

// Trace
export interface SpanEvent {
  timeUnixNano: string;
  name: string;
  attributes: Record<string, unknown>;
}

export interface SpanDto {
  spanId: string;
  parentSpanId: string | null;
  serviceName: string;
  operation: string;
  status: SpanStatus;
  durationMs: number;
  startedAt: string;
  endedAt: string;
  attributes: Record<string, unknown>;
  events: SpanEvent[];
}

export interface TraceDto {
  id: string;
  traceId: string;
  rootService: string;
  rootOperation: string;
  status: TraceStatus;
  failureType: FailureType;
  durationMs: number;
  startedAt: string;
  endedAt: string;
  spanCount: number;
  createdAt: string;
  spans?: SpanDto[];  // present on detail view only
}

// Alert
export interface AlertDto {
  id: string;
  alertId: string;
  traceId: string;
  severity: AlertSeverity;
  status: AlertStatus;
  title: string;
  description: string;
  triggeredAt: string;
  updatedAt: string;
}

// AI Analysis
export interface AnalysisDto {
  id: string;
  analysisId: string;
  traceId: string;
  alertId: string | null;
  status: AnalysisStatus;
  rootCause: string | null;
  affectedServices: string[];
  recommendations: string[];
  confidenceScore: number | null;
  modelUsed: string | null;
  createdAt: string;
  completedAt: string | null;
}

// BFF Aggregated Incident
export interface IncidentDto {
  trace: TraceDto;
  alert: AlertDto | null;
  analysis: AnalysisDto | null;
}

// Dashboard Summary
export interface DashboardSummaryDto {
  totalTraces: number;
  errorTraces: number;
  openAlerts: number;
  criticalAlerts: number;
  completedAnalyses: number;
  avgTraceDurationMs: number;
  lastUpdated: string;
}

// Paginated Response
export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

// WebSocket Messages
export interface WsAlertMessage {
  type: "ALERT_CREATED";
  alertId: string;
  traceId: string;
  severity: AlertSeverity;
  status: AlertStatus;
  title: string;
  description: string;
  triggeredAt: string;
}

export interface WsAnalysisMessage {
  type: "ANALYSIS_COMPLETED";
  analysisId: string;
  traceId: string;
  alertId: string | null;
  status: AnalysisStatus;
  rootCause: string;
  recommendations: string[];
  confidenceScore: number;
  completedAt: string;
}

export interface WsTraceMessage {
  type: "TRACE_INGESTED";
  traceId: string;
  rootService: string;
  rootOperation: string;
  status: TraceStatus;
  failureType: FailureType;
  durationMs: number;
  startedAt: string;
}
```

---

## 13. Folder Structure

### 13.1 trace-service (Java Spring Boot)

```
trace-service/
├── src/main/java/com/incident/trace/
│   ├── com.trace-service.TraceServiceApplication.java
│   ├── config/
│   │   ├── KafkaProducerConfig.java
│   │   └── DatabaseConfig.java
│   ├── controller/
│   │   └── TraceController.java
│   ├── service/
│   │   ├── TraceIngestionService.java
│   │   └── TraceQueryService.java
│   ├── kafka/
│   │   └── TraceEventProducer.java
│   ├── repository/
│   │   ├── TraceRepository.java
│   │   └── SpanRepository.java
│   ├── model/
│   │   ├── entity/
│   │   │   ├── TraceEntity.java
│   │   │   └── SpanEntity.java
│   │   ├── dto/
│   │   │   ├── TraceDto.java
│   │   │   ├── SpanDto.java
│   │   │   └── TraceIngestResponse.java
│   │   ├── kafka/
│   │   │   └── TraceIngestedEvent.java
│   │   └── enums/
│   │       ├── TraceStatus.java
│   │       ├── SpanStatus.java
│   │       └── FailureType.java
│   └── mapper/
│       └── TraceMapper.java
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/
│       ├── V1__create_traces.sql
│       └── V2__create_spans.sql
└── Dockerfile
```

### 13.2 alert-service (Java Spring Boot)

```
alert-service/
├── src/main/java/com/incident/alert/
│   ├── AlertServiceApplication.java
│   ├── config/
│   │   ├── KafkaConsumerConfig.java
│   │   └── RedisConfig.java
│   ├── controller/
│   │   └── AlertController.java
│   ├── service/
│   │   ├── AlertCreationService.java
│   │   └── AlertQueryService.java
│   ├── kafka/
│   │   └── TraceEventConsumer.java
│   ├── redis/
│   │   └── AlertPublisher.java
│   ├── repository/
│   │   └── AlertRepository.java
│   ├── model/
│   │   ├── entity/AlertEntity.java
│   │   ├── dto/AlertDto.java
│   │   ├── kafka/TraceIngestedEvent.java
│   │   └── enums/
│   │       ├── AlertSeverity.java
│   │       └── AlertStatus.java
│   └── mapper/AlertMapper.java
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/
│       └── V1__create_alerts.sql
└── Dockerfile
```

### 13.3 bff-service (Java Spring WebFlux)

```
bff-service/
├── src/main/java/com/incident/bff/
│   ├── BffServiceApplication.java
│   ├── config/
│   │   ├── WebClientConfig.java
│   │   ├── RedisConfig.java
│   │   └── WebSocketConfig.java
│   ├── controller/
│   │   ├── DashboardController.java
│   │   ├── IncidentController.java
│   │   ├── TraceProxyController.java
│   │   └── AlertProxyController.java
│   ├── service/
│   │   ├── IncidentAggregationService.java
│   │   └── DashboardService.java
│   ├── websocket/
│   │   ├── WebSocketRelay.java
│   │   └── RedisSubscriber.java
│   └── model/dto/
│       ├── IncidentDto.java
│       └── DashboardSummaryDto.java
├── src/main/resources/application.yml
└── Dockerfile
```

### 13.4 order-simulator (Java Spring Boot)

```
order-simulator/
├── src/main/java/com/incident/simulator/
│   ├── OrderSimulatorApplication.java
│   ├── controller/SimulationController.java
│   ├── service/OrderSimulationService.java
│   ├── otel/
│   │   └── TraceSpanBuilder.java
│   └── model/
│       ├── SimulateRequest.java
│       └── SimulateResponse.java
├── src/main/resources/application.yml
└── Dockerfile
```

### 13.5 ai-analysis-service (Python FastAPI)

```
ai-analysis-service/
├── app/
│   ├── main.py
│   ├── config.py
│   ├── models/
│   │   ├── enums.py
│   │   ├── schemas.py          # Pydantic models
│   │   └── database.py         # SQLAlchemy models
│   ├── api/
│   │   └── routes/
│   │       └── analysis.py
│   ├── services/
│   │   ├── analysis_service.py
│   │   └── llm_service.py
│   ├── kafka/
│   │   └── consumer.py
│   ├── redis/
│   │   └── publisher.py
│   ├── db/
│   │   └── repository.py
│   └── prompts/
│       └── root_cause_prompt.py  # Frozen prompt templates
├── requirements.txt
├── Dockerfile
└── .env.example
```

### 13.6 frontend (React + TypeScript)

```
frontend/
├── src/
│   ├── main.tsx
│   ├── App.tsx
│   ├── types/
│   │   ├── enums.ts
│   │   └── dtos.ts             # All DTO types from Section 12.3
│   ├── api/
│   │   ├── client.ts           # Axios/fetch base client
│   │   ├── traces.ts
│   │   ├── alerts.ts
│   │   ├── incidents.ts
│   │   ├── dashboard.ts
│   │   └── simulate.ts
│   ├── hooks/
│   │   ├── useWebSocket.ts
│   │   ├── useIncidents.ts
│   │   └── useDashboard.ts
│   ├── pages/
│   │   ├── Dashboard.tsx
│   │   ├── IncidentsList.tsx
│   │   ├── IncidentDetail.tsx
│   │   └── Simulate.tsx
│   ├── components/
│   │   ├── layout/
│   │   │   ├── Navbar.tsx
│   │   │   └── Layout.tsx
│   │   ├── trace/
│   │   │   ├── TraceWaterfall.tsx
│   │   │   └── SpanRow.tsx
│   │   ├── alert/
│   │   │   ├── AlertCard.tsx
│   │   │   └── AlertBadge.tsx
│   │   ├── analysis/
│   │   │   └── AnalysisPanel.tsx
│   │   └── common/
│   │       ├── StatusBadge.tsx
│   │       └── LoadingSpinner.tsx
│   └── utils/
│       ├── formatters.ts
│       └── constants.ts
├── package.json
├── tsconfig.json
├── vite.config.ts
└── Dockerfile
```

---

## 14. Sequence Diagrams

### 14.1 Happy Path — Failure Injected, End-to-End

```
Frontend          BFF             Simulator        TraceService      Kafka
   |                |                |                |                |
   |--POST /simulate→|               |                |                |
   |                |--POST /simulate→|               |                |
   |                |               |--OTel spans--→ |                |
   |                |               |  (OTLP/HTTP)   |                |
   |                |               |                |--INSERT traces |
   |                |               |                |   spans to PG  |
   |                |               |                |--PRODUCE ----→ |
   |                |               |                |  trace-events  |
   |                |               |                |                |
   |←─SimulateResp──|               |                |                |

          AlertService      Redis             AI-Service        WebSocket
               |               |                  |                |
               |←─CONSUME ─────────────────────── |               |
               |   trace-events                   |               |
               |                                  |←─CONSUME ─────|
               |                                  |  trace-events  |
               |--INSERT alert to PG              |                |
               |--PUBLISH alerts:live--→|         |                |
               |               |--relay→|         |                |
               |               |        |------WebSocket /topic/alerts→ Frontend
               |               |        |         |                |
               |               |        | AI calls LLM             |
               |               |        |--INSERT analysis to PG   |
               |               |        |--PUBLISH analysis:live→ |
               |               |        |         |--relay→--------→ Frontend
               |               |        |         |                |
```

### 14.2 Frontend Loads Incident Detail

```
Frontend                    BFF                TraceService    AlertService    AI Service
   |                         |                      |               |               |
   |--GET /incidents/:tid ---→|                      |               |               |
   |                         |--GET /traces/:tid ---→|               |               |
   |                         |←─TraceDto with spans--               |               |
   |                         |--GET /alerts?traceId=:tid -----------→|               |
   |                         |←─AlertDto ──────────────────────────                 |
   |                         |--GET /analyses/:tid ─────────────────────────────────→|
   |                         |←─AnalysisDto ──────────────────────────────────────── |
   |                         |                      |               |               |
   |←──IncidentDto (merged)──|                      |               |               |
   |                         |                      |               |               |
```

### 14.3 WebSocket Real-Time Update Flow

```
AlertService        Redis Pub/Sub      BFF (WebFlux)          Frontend
     |                    |                  |                     |
     |--PUBLISH ─────────→|                  |                     |
     |  alerts:live       |--MESSAGE ───────→|                     |
     |                    |  (JSON)          |--STOMP SEND ───────→|
     |                    |                  |  /topic/alerts       |
     |                    |                  |                      |
     |                    |                  | Frontend adds alert  |
     |                    |                  | to UI live feed      |
```

---

## 15. API Versioning Strategy

**Strategy:** URI path versioning (`/api/v1/...`)

**Rationale:** Simple, cache-friendly, visible in logs, appropriate for portfolio demo scope.

### Rules

1. All services expose `/api/v1/` prefix on every endpoint — no exceptions.
2. For this 2-week project, **only v1 exists**. No v2 endpoints during development.
3. Version is encoded in path, not headers (`Accept: application/vnd.v1+json` is out of scope).
4. If a breaking change is required mid-sprint, the version is bumped in the contract document first, then all consumers update simultaneously (no parallel versions in demo).

### Java Convention

```java
@RequestMapping("/api/v1/traces")
public class TraceController { }
```

### Python Convention

```python
router = APIRouter(prefix="/api/v1")
```

### Frontend Convention

```typescript
const API_BASE = "/api/v1";  // in constants.ts
```

---

## 16. Contract-First Development Recommendations

### 16.1 Day 1 Checklist (Before Any Code)

- [ ] Both developers read and sign off on this document
- [ ] All enum values agreed and copied into each language
- [ ] PostgreSQL schema reviewed — no changes after Day 2
- [ ] Kafka topic names confirmed in docker-compose
- [ ] LLM provider and model name finalized in Section 11.3
- [ ] WebSocket STOMP topic paths confirmed
- [ ] BFF aggregation contract (`/incidents/:traceId`) confirmed by both devs

### 16.2 Mock Strategy

**Dev B (Python) can develop AI service without waiting for Dev A:**

1. Create a mock Kafka producer script (`scripts/mock_kafka_producer.py`) that publishes a `TRACE_INGESTED` event matching Section 7.2.
2. Use a local PostgreSQL with the schema from Section 4 pre-loaded with seed data.
3. Start AI service development on Day 1 independently.

**Dev A can develop Alert Service without AI service:**

1. Alert service publishes to Redis regardless of AI service state.
2. BFF returns `null` for `analysis` field when AI service hasn't responded yet — gracefully handled by frontend.

### 16.3 Integration Test Points

These must be verified before frontend integration (end of Week 1):

| # | Test | Pass Criteria |
|---|---|---|
| 1 | Simulator → TraceService | Trace appears in DB with correct spans |
| 2 | TraceService → Kafka | `trace-events` message has correct schema |
| 3 | Kafka → AlertService | Alert row created in DB for ERROR trace |
| 4 | AlertService → Redis | `alerts:live` channel receives message |
| 5 | Kafka → AI Service | Analysis row created, LLM called |
| 6 | AI Service → Redis | `analysis:live` channel receives COMPLETED message |
| 7 | BFF `/incidents/:traceId` | Returns trace + alert + analysis aggregated |
| 8 | BFF WebSocket | Frontend receives live alert via STOMP |

### 16.4 Frozen Contracts (No Changes Without Team Agreement)

The following cannot be changed unilaterally after Day 2:

- Kafka event schemas (Section 7)
- PostgreSQL schema (Section 4)
- Redis channel names and message shapes (Section 8)
- BFF aggregated incident DTO (Section 9.4)
- WebSocket STOMP topic paths (Section 10)
- AI prompt format and LLM response JSON schema (Section 11.2)

### 16.5 What to Cut If Behind Schedule

In priority order (cut from bottom up):

| Priority | Feature | Cut Impact |
|---|---|---|
| MUST | Trace ingestion + storage | Demo dies |
| MUST | Kafka → Alert creation | Demo dies |
| MUST | AI analysis (basic) | Core value prop |
| MUST | WebSocket live updates | Key demo wow factor |
| SHOULD | Dashboard summary stats | Still looks good |
| SHOULD | Trace span waterfall | Nice to have |
| COULD | Alert acknowledge/resolve | Trivial |
| COULD | Incident list pagination | Use size=100 and skip |
| CUT | `/simulate` endpoint | Trigger via curl instead |

---

## 17. Dependency & Boot Order

### 17.1 docker-compose Boot Dependencies

```
postgres  ──→  trace-service
              alert-service
              ai-analysis-service

kafka     ──→  trace-service (produces)
              alert-service (consumes)
              ai-analysis-service (consumes)

redis     ──→  alert-service (publishes)
              bff-service (subscribes)
              ai-analysis-service (publishes)

trace-service     ──→  bff-service
alert-service     ──→  bff-service
ai-analysis-svc   ──→  bff-service

bff-service       ──→  frontend
```

### 17.2 Health Check Endpoints

All Java services expose:  
`GET /actuator/health` → `{ "status": "UP" }`

Python AI service exposes:  
`GET /health` → `{ "status": "ok" }`

### 17.3 Environment Variables Summary

```env
# Shared (all services)
POSTGRES_HOST=postgres
POSTGRES_PORT=5432
POSTGRES_DB=incidents
POSTGRES_USER=incidents
POSTGRES_PASSWORD=incidents

KAFKA_BOOTSTRAP_SERVERS=kafka:9092
REDIS_HOST=redis
REDIS_PORT=6379

# BFF only
TRACE_SERVICE_URL=http://trace-service:8082
ALERT_SERVICE_URL=http://alert-service:8083
AI_SERVICE_URL=http://ai-analysis-service:8090

# AI Service only
LLM_PROVIDER=openai
OPENAI_API_KEY=<secret>
ANTHROPIC_API_KEY=<secret>
```

---

## Appendix A — Interview Talking Points

These contracts demonstrate the following to interviewers:

| Concept | Where Demonstrated |
|---|---|
| Event-driven architecture | Kafka trace-events + alert-events topics |
| Async microservices | alert-service and ai-service as independent Kafka consumers |
| Reactive programming | Spring WebFlux BFF, Redis Pub/Sub → WebSocket relay |
| Contract-first API design | This document; frozen before code |
| Observability primitives | OTel span model, trace waterfall |
| LLM integration | Prompt design, structured output, confidence scoring |
| Real-time UX | WebSocket STOMP push to frontend |
| Data modeling | PostgreSQL schema with indexes, JSONB for attributes |
| Separation of concerns | BFF aggregation pattern; services own one table each |
| Resilience patterns | Graceful null for pending analysis; cut list planning |

---

*Document Version: 1.0.0 — FROZEN*  
*All changes require both developers to review and version bump.*