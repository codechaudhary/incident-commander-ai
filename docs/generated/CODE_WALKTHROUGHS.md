# CODE WALKTHROUGHS

This document provides step-by-step walkthroughs of the critical system flows, tracing the exact sequence of method calls across microservices.

## Walkthrough 1: The Lifecycle of an Error Trace

**Scenario:** The `order-simulator` sends a payload representing a database timeout.

1. **Ingestion (`trace-service`)**
   - HTTP POST hits `TraceController.java`.
   - Payload is passed to `TraceIngestionServiceImpl.java`.
   - The service extracts the span tree. Because one span has `status="TIMEOUT"`, the root trace is marked as `TIMEOUT` with `FailureType="DB_CONNECTION_ERROR"`.
   - `TraceRepository` and `SpanRepository` save the entities to PostgreSQL.
   - `TracePersistedApplicationEvent` is fired internally.
   - `TracePersistedEventListener` intercepts it post-commit and uses `KafkaTraceEventProducer` to publish a `TraceIngestedPayload` to the `trace-ingested-events` Kafka topic.

2. **Alert Evaluation (`alert-service`)**
   - `TraceEventConsumer.java` pulls the event from Kafka.
   - It sees `status == "TIMEOUT"` (which is in the `ALERTABLE_STATUSES` set).
   - `AlertCreationService.java` executes. It maps `DB_CONNECTION_ERROR` to `Severity.HIGH`.
   - The `AlertEntity` is saved to PostgreSQL, defaulting to `status="OPEN"`.
   - `AlertEventProducer` publishes an event to Kafka (though nothing currently consumes it).

3. **AI Root Cause Analysis (`ai-analytics-service`)**
   - In parallel to step 2, `consumer.py` pulls the same event from the `trace-ingested-events` topic.
   - `AnalysisService.process_trace_event` is invoked.
   - It creates a `PENDING` record in Redis via `redis_repository.py`.
   - `LLMService.analyze` invokes the Anthropic API, injecting the trace duration and failure type into `root_cause_prompt.py`.
   - Once the HTTPX async call returns, the Redis record is updated to `COMPLETED`.
   - `AnalysisPublisher` uses Redis to broadcast the resulting `AnalysisDto` to the `analysis:live` channel.

4. **Dashboard Real-Time Update (`bff-service`)**
   - `RedisSubscriberConfig.java` receives the event on `analysis:live`.
   - `RedisMessageRelay.java` pushes the payload down the WebSocket to `/topic/analysis`.
   - The React frontend receives the WebSocket message and dynamically updates the incident card to show the LLM narrative.

## Walkthrough 2: Dashboard Aggregation (Scatter-Gather)

**Scenario:** A user hits refresh on the main dashboard.

1. HTTP GET hits `DashboardController.java` (`/api/v1/dashboard/summary`) in the `bff-service`.
2. `DashboardService.java` uses `Mono.zip()` to fire 6 concurrent WebClient requests:
   - `TraceClient` -> `GET /api/v1/traces?size=1`
   - `TraceClient` -> `GET /api/v1/traces?status=ERROR&size=1`
   - `AlertClient` -> `GET /api/v1/alerts?status=OPEN&size=1`
   - `AlertClient` -> `GET /api/v1/alerts?severity=CRITICAL&size=1`
   - `AiClient`    -> `GET /api/v1/analyses?status=COMPLETED&size=1`
   - `TraceClient` -> `GET /api/v1/traces?size=20` (to compute average duration manually).
3. The Reactor Netty event loop waits asynchronously.
4. As JSON responses return, `getCount()` parses the `totalElements` field from each Spring Data page metadata.
5. Once all 6 Monos complete, they are zipped into a `DashboardSummaryResponse` and returned as a single JSON to the frontend.
