# LOW LEVEL DESIGN: bff-service

## 1. Class Architecture & Component Interaction

### Controllers
- **`DashboardController.java`**
  - `GET /api/v1/dashboard/summary`: Calls `DashboardService.getSummary()` to retrieve aggregate metrics.
- **`IncidentController.java`**
  - `GET /api/v1/incidents/{traceId}`: Calls `IncidentService.getIncidentView()` to merge trace, alert, and analysis data.
- **`ProxyController.java`**
  - Proxy endpoints for `/traces` and `/alerts` using standard WebClient mappings.

### Core Services
- **`DashboardService.java`**
  - Uses `Mono.zip()` to execute 6 concurrent HTTP calls.
  - Computes `avgTraceDurationMs` on the fly by parsing the first page of traces.
- **`IncidentService.java`**
  - Merges `TraceDetailResponse`, `AlertResponse`, and `AnalysisDto` into an `IncidentViewResponse`.

### Clients
- **`AiClient.java`**
  - `triggerAnalysis(String traceId, JsonNode traceData)` -> Posts to `/api/v1/analyses/trigger`.
  - Maps exceptions using `.onErrorResume(e -> Mono.empty())`.
- **`TraceClient.java`** & **`AlertClient.java`**
  - Abstract web client wrappers with identical fallback semantics.

### Real-Time Infrastructure
- **`RedisSubscriberConfig.java`**
  - Bootstraps `ReactiveRedisMessageListenerContainer`.
- **`RedisMessageRelay.java`**
  - Contains switch logic mapping Redis channels to WebSocket paths (`alerts:live` -> `/topic/alerts`).
- **`WebSocketConfig.java`**
  - Enables simple broker for `/topic`. Defines endpoint `/ws`.
