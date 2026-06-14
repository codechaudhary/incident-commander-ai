# AI Analytics Service

FastAPI backend for the Incident Commander AI analysis pipeline. It consumes trace events,
generates root-cause analysis with an LLM provider, stores analysis records in PostgreSQL,
publishes completed analysis updates to Redis, and exposes HTTP APIs for reading or queuing
analysis work.

## Tech Stack

- Python 3.14
- FastAPI + Uvicorn
- Pydantic v2 for request/response contracts and settings
- SQLAlchemy async ORM with `asyncpg`
- PostgreSQL for persisted analysis records
- Kafka via `aiokafka` for trace-event ingestion
- Redis Pub/Sub for live analysis-completed notifications
- `httpx` for OpenCode Zen LLM calls
- Ruff and Pytest for development checks

## Runtime Entry Point

The service is designed to run directly from the service root:

```powershell
python app/main.py
```

`app/main.py` creates the FastAPI app, configures logging, wires the service dependencies, and
starts Uvicorn using `HOST` and `PORT` from `.env`.

Default local URL:

```text
http://localhost:8090
```

If port `8090` is already in use, either stop the existing process or change `PORT` in `.env`.

## High-Level Flow

1. A trace event is produced to Kafka topic `trace-events`.
2. `TraceEventConsumer` reads messages when `KAFKA_CONSUMER_ENABLED=true`.
3. The message is validated as a `KafkaEnvelope`.
4. Only `TRACE_INGESTED` events are processed.
5. Successful traces are skipped.
6. Error or timeout traces create an `ai_analysis` row with `PENDING`, then `PROCESSING`.
7. `LLMService` analyzes the trace.
8. The LLM response is validated against the frozen JSON contract.
9. The database row is updated to `COMPLETED` or `FAILED`.
10. Completed results are published to Redis channel `analysis:live`.
11. API clients can fetch analysis status/results by `traceId`.

## Application Components

| Area | File | Responsibility |
| --- | --- | --- |
| App startup | `app/main.py` | FastAPI app, lifespan, health routes, dependency wiring |
| Config | `app/core/config.py` | `.env` loading and typed runtime settings |
| API routes | `app/api/routes/analysis.py` | Analysis HTTP endpoints |
| Domain service | `app/services/analysis_service.py` | Orchestrates DB, LLM, and Redis publishing |
| LLM client | `app/services/llm_service.py` | OpenCode Zen calls, fallback models, stub mode |
| Kafka consumer | `app/kafka/consumer.py` | Consumes and validates trace events |
| DB repository | `app/db/repository.py` | Analysis row creation and status updates |
| DB session | `app/db/session.py` | Async SQLAlchemy engine/session factory |
| Redis publisher | `app/redis/publisher.py` | Publishes completed analysis messages |
| Schemas | `app/models/schemas.py` | Pydantic API and event contracts |
| DB model | `app/models/database.py` | `ai_analysis` table mapping |

## HTTP API

Base API prefix is controlled by `API_PREFIX`, default:

```text
/api/v1
```

### `GET /health`

Basic process health check. Does not require database connectivity.

Response:

```json
{
  "status": "ok"
}
```

### `GET /ready`

Readiness check. Runs `SELECT 1` against PostgreSQL.

Response:

```json
{
  "status": "ok"
}
```

If PostgreSQL is unavailable, this endpoint fails.

### `GET /api/v1/analyses/{trace_id}`

Returns the latest analysis record for a trace.

If analysis is still running, response status is `202 Accepted`:

```json
{
  "analysisId": "uuid",
  "traceId": "trace-123",
  "status": "PROCESSING",
  "message": "Analysis in progress"
}
```

If analysis is complete, response status is `200 OK`:

```json
{
  "id": "uuid",
  "analysisId": "uuid",
  "traceId": "trace-123",
  "alertId": "alert-123",
  "status": "COMPLETED",
  "rootCause": "Database timeout caused order creation to fail.",
  "affectedServices": ["orders", "database"],
  "recommendations": ["Check database capacity."],
  "confidenceScore": 0.9,
  "modelUsed": "deepseek-v4-flash-free",
  "promptTokens": 100,
  "completionTokens": 80,
  "createdAt": "2026-06-14T07:25:34Z",
  "completedAt": "2026-06-14T07:25:38Z"
}
```

If no record exists, returns `404`.

### `POST /api/v1/analyses/trigger`

Creates or returns a pending analysis record for a trace. This endpoint queues the database
record only; actual trace processing depends on Kafka/event ingestion.

Request:

```json
{
  "traceId": "trace-123",
  "alertId": "alert-123"
}
```

Response status: `202 Accepted`

```json
{
  "analysisId": "uuid",
  "traceId": "trace-123",
  "status": "PENDING",
  "message": "Analysis queued"
}
```

## Kafka Event Contract

The consumer expects a Kafka message matching `KafkaEnvelope`.

```json
{
  "eventId": "00000000-0000-0000-0000-000000000000",
  "eventType": "TRACE_INGESTED",
  "eventVersion": "1.0",
  "timestamp": "2026-06-14T07:25:34Z",
  "source": "trace-ingestion-service",
  "payload": {
    "traceId": "trace-123",
    "rootService": "orders",
    "rootOperation": "POST /orders",
    "status": "ERROR",
    "failureType": "RUNTIME_EXCEPTION",
    "durationMs": 1200,
    "startedAt": "2026-06-14T07:25:30Z",
    "endedAt": "2026-06-14T07:25:31Z",
    "spanCount": 3,
    "errorSpans": [
      {
        "spanId": "span-1",
        "serviceName": "orders",
        "operation": "insert_order",
        "errorMessage": "database timeout",
        "durationMs": 900
      }
    ]
  }
}
```

Valid trace statuses:

- `SUCCESS`
- `ERROR`
- `TIMEOUT`

Valid failure types:

- `NONE`
- `SLOW_RESPONSE`
- `DB_TIMEOUT`
- `RUNTIME_EXCEPTION`

## LLM Behavior

The service supports two LLM modes:

| Provider | Behavior |
| --- | --- |
| `stub` | Deterministic local heuristic response, useful for development/tests |
| `opencode` | Calls OpenCode Zen chat-completions API |

When `LLM_PROVIDER=opencode`, `OPENCODE_API_KEY` is required.

Configured fallback model order:

1. `deepseek-v4-flash-free`
2. `qwen3.6-plus-free`
3. `minimax-m3-free`
4. `nemotron-3-ultra-free`

If one model fails due to HTTP, timeout, invalid response, or schema validation error, the
service logs the failure and tries the next model. If all models fail, the analysis is marked
`FAILED`.

Expected LLM JSON contract:

```json
{
  "rootCause": "string",
  "affectedServices": ["service-name"],
  "recommendations": ["actionable recommendation"],
  "confidenceScore": 0.0
}
```

## Database

Table: `ai_analysis`

| Column | Purpose |
| --- | --- |
| `id` | Primary UUID |
| `analysis_id` | Public analysis ID |
| `trace_id` | Trace being analyzed |
| `alert_id` | Optional linked alert |
| `status` | `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED` |
| `root_cause` | LLM/root-cause summary or failure message |
| `affected_services` | Services affected by the incident |
| `recommendations` | Actionable remediation steps |
| `confidence_score` | LLM confidence from `0.0` to `1.0` |
| `model_used` | LLM model that completed analysis |
| `prompt_tokens` | Prompt token usage, when returned |
| `completion_tokens` | Completion token usage, when returned |
| `created_at` | Row creation timestamp |
| `completed_at` | Completion/failure timestamp |

## Redis Publishing

When an analysis completes, the service publishes this message to `REDIS_ANALYSIS_CHANNEL`
defaulting to `analysis:live`:

```json
{
  "type": "ANALYSIS_COMPLETED",
  "analysisId": "uuid",
  "traceId": "trace-123",
  "alertId": "alert-123",
  "status": "COMPLETED",
  "rootCause": "Database timeout caused order creation to fail.",
  "recommendations": ["Check database capacity."],
  "confidenceScore": 0.9,
  "completedAt": "2026-06-14T07:25:38Z"
}
```

## Configuration

Settings are loaded from `.env`.

| Variable | Default | Description |
| --- | --- | --- |
| `APP_NAME` | `ai-analysis-service` | Service name in logs |
| `ENVIRONMENT` | `local` | Runtime environment |
| `LOG_LEVEL` | `INFO` | Logging level |
| `API_PREFIX` | `/api/v1` | Prefix for API routes |
| `HOST` | `0.0.0.0` | Uvicorn bind host |
| `PORT` | `8090` | Uvicorn bind port |
| `DATABASE_URL` | Postgres DSN | PostgreSQL connection URL |
| `KAFKA_BOOTSTRAP_SERVERS` | `kafka:9092` | Kafka broker list |
| `KAFKA_TRACE_TOPIC` | `trace-events` | Trace event topic |
| `KAFKA_CONSUMER_GROUP` | `ai-analysis-group` | Kafka consumer group |
| `KAFKA_CONSUMER_ENABLED` | `false` in local env | Enables Kafka background consumer |
| `REDIS_URL` | `redis://redis:6379` | Redis connection URL |
| `REDIS_ANALYSIS_CHANNEL` | `analysis:live` | Completed-analysis Pub/Sub channel |
| `LLM_PROVIDER` | `opencode` in `.env` | `opencode` or `stub` |
| `OPENCODE_API_KEY` | empty | Required for OpenCode mode |
| `OPENCODE_BASE_URL` | `https://opencode.ai/zen/v1` | OpenCode API base URL |
| `OPENCODE_MODELS` | fallback list | Comma-separated model fallback order |
| `LLM_MAX_TOKENS` | `500` | Max completion tokens |
| `LLM_TEMPERATURE` | `0.2` | Model temperature |
| `LLM_TIMEOUT_SECONDS` | `30` | HTTP timeout |

## Local Development

Create and activate a virtual environment, install dev requirements, then run the app:

```powershell
python -m venv venv
.\venv\Scripts\activate
pip install -r requirements-dev.txt
python app/main.py
```

Useful local settings:

```env
KAFKA_CONSUMER_ENABLED=false
LLM_PROVIDER=stub
PORT=8090
```

Use `LLM_PROVIDER=opencode` and set `OPENCODE_API_KEY` when testing real model calls.

## Checks

Run tests:

```powershell
python -m pytest
```

Run lint:

```powershell
python -m ruff check app tests
```

## Common Issues

### Port already in use

Error:

```text
WinError 10048 only one usage of each socket address is normally permitted
```

Fix by stopping the process using the port or changing `PORT` in `.env`.

```powershell
Get-NetTCPConnection -LocalPort 8090 -State Listen
Stop-Process -Id <PID>
```

### Kafka unavailable locally

Keep this disabled unless Kafka is running:

```env
KAFKA_CONSUMER_ENABLED=false
```

### OpenCode key missing

If `LLM_PROVIDER=opencode`, set:

```env
OPENCODE_API_KEY=your-key
```

For offline/local tests, use:

```env
LLM_PROVIDER=stub
```
