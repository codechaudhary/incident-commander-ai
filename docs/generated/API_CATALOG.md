# API CATALOG

This document lists all internal and external REST API endpoints exposed by the microservices, based strictly on codebase annotations.

## 1. Trace Service (`trace-service`)
**Base Path:** `/api/v1/traces`

| Method | Path | Purpose | Request Body | Response Body |
|---|---|---|---|---|
| `POST` | `/` | Ingest an OTLP-like trace payload | `OtlpTraceRequest` | `TraceIngestResponse` (202 Accepted) |
| `GET` | `/` | Fetch paginated traces with filters | `status`, `page`, `size` (Query Params) | `PagedResponse<TraceSummaryResponse>` |
| `GET` | `/{traceId}` | Fetch full trace details including span tree | None | `TraceDetailResponse` |

## 2. Alert Service (`alert-service`)
**Base Path:** `/api/v1/alerts`

| Method | Path | Purpose | Request Body | Response Body |
|---|---|---|---|---|
| `GET` | `/` | Fetch paginated alerts with filters | `status`, `severity`, `page`, `size` | `PagedResponse<AlertResponse>` |
| `GET` | `/{alertId}` | Fetch alert details | None | `AlertResponse` |
| `PATCH`| `/{alertId}/status` | Update alert status (ACK, RESOLVE) | `{"status": "..."}` | `AlertResponse` |

## 3. AI Analytics Service (`ai-analytics-service`)
**Base Path:** `/api/v1/analyses`

| Method | Path | Purpose | Request Body | Response Body |
|---|---|---|---|---|
| `POST` | `/trigger` | Force trigger an LLM analysis for a trace | `TriggerAnalysisRequest` | `PendingAnalysisResponse` (202 Accepted) |
| `GET` | `/{traceId}` | Get analysis status or result by trace ID | None | `AnalysisDto` OR `PendingAnalysisResponse` |

## 4. BFF Service (`bff-service`)
**Base Path:** `/api/v1`

| Method | Path | Purpose | Upstream Routing |
|---|---|---|---|
| `GET` | `/dashboard/summary` | Fetch aggregated counts and stats | Calls Trace, Alert, and AI services concurrently |
| `GET` | `/incidents/{traceId}` | Fetch unified incident view | Calls Trace, Alert, and AI services |
| `GET` | `/traces` | Proxy to Trace Service | Proxies `GET /api/v1/traces` |
| `GET` | `/alerts` | Proxy to Alert Service | Proxies `GET /api/v1/alerts` |

## 5. Order Simulator (`order-simulator`)
**Base Path:** `/api/v1`

| Method | Path | Purpose | Request Body |
|---|---|---|---|
| `POST` | `/simulate` | Generate synthetic trace data | `{"failureType": "TIMEOUT|ERROR|NONE"}` |
