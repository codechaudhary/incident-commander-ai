# MASTER INTERVIEW GUIDE

This document contains deep technical interview questions based strictly on the actual implementation of the Incident Commander AI architecture. Use these to test candidates on their real understanding of the system.

## 1. Concurrency & WebFlux
**Question:** How does the dashboard retrieve all its metrics simultaneously without blocking Tomcat threads?
**Expected Answer:** The `bff-service` uses Spring WebFlux (Reactor). The `DashboardService.java` utilizes `Mono.zip()` to execute 6 concurrent non-blocking HTTP requests via `WebClient` to the downstream Trace, Alert, and AI services. Because it uses Netty underneath, no OS threads are blocked while waiting for the downstream IO to complete.

## 2. Event Idempotency
**Question:** The `trace.ingested` topic uses at-least-once delivery semantics. How does the `alert-service` prevent creating duplicate alerts if Kafka replays a message?
**Expected Answer:** The `alert-service` relies on database-level constraints. The `alerts` table has a `UNIQUE` constraint on the `trace_id` (Wait, actually the constraint is on `alert_id`, but the code throws `DuplicateAlertException` if `trace_id` already exists). The `TraceEventConsumer` wraps the insertion in a try-catch block and silently swallows `DuplicateAlertException`, acknowledging the Kafka offset without creating a duplicate.

## 3. Distributed Transactions
**Question:** When the `trace-service` receives an HTTP payload, it saves to PostgreSQL and emits a Kafka event. How does it prevent emitting the Kafka event if the database transaction rolls back (e.g., due to a constraint violation)?
**Expected Answer:** It utilizes Spring's `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`. The `TraceIngestionServiceImpl` fires an internal Spring ApplicationEvent (`TracePersistedApplicationEvent`), but the listener that actually calls the `KafkaTemplate` waits until the JPA transaction successfully commits before firing.

## 4. WebSockets & Pub/Sub
**Question:** Why do we use Redis Pub/Sub in the `bff-service` instead of directly sending messages from the backend services to the WebSocket?
**Expected Answer:** The `bff-service` acts as the API Gateway and terminates the WebSocket connections. Because we could run multiple instances of the `bff-service` behind a load balancer, an incoming Kafka event processed by the `ai-analytics-service` wouldn't know which BFF instance holds the user's WebSocket session. By publishing to Redis, all BFF instances receive the payload and broadcast it to whichever clients they are currently connected to.

## 5. Python AsyncIO
**Question:** How does the `ai-analytics-service` handle a REST trigger without blocking the HTTP response while the LLM takes 10 seconds to generate a narrative?
**Expected Answer:** It utilizes FastAPI `BackgroundTasks`. The `TriggerAnalysisRequest` creates a `PENDING` database record immediately, kicks off the `process_from_trigger` task in the background asyncio event loop, and immediately returns an HTTP 202 Accepted. The LLM call uses `httpx.AsyncClient` so it doesn't block the event loop either.
