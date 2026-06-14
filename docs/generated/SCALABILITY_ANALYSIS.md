# SCALABILITY ANALYSIS

This document analyzes the scalability constraints and bottlenecks of the current architecture.

## 1. Trace Ingestion Bottleneck
- **Synchronous Persistence:** The `trace-service` exposes an HTTP POST endpoint for ingestion. Within a single blocking thread, it:
  1. Validates the JSON payload.
  2. Parses the span tree into entities.
  3. Executes an `INSERT` into the PostgreSQL `traces` table.
  4. Executes batch `INSERT`s into the PostgreSQL `spans` table.
  5. Finally, commits the transaction and triggers the Kafka event.
- **Risk:** At high load (e.g., a major cascading incident generating 50,000 trace segments per minute), database connection pools will exhaust, and the HTTP listener will reject connections. A highly scalable trace ingestion system typically drops raw HTTP payloads directly onto a Kafka topic and defers the database parsing/insertion to async consumer groups.

## 2. API Gateway Concurrency
- **Spring WebFlux:** The `bff-service` uses reactive, non-blocking I/O. Its `DashboardService.getSummary()` method executes 6 parallel HTTP requests using `Mono.zip()`.
- **Strength:** This is highly scalable. A single BFF instance can handle thousands of concurrent dashboard users without exhausting Tomcat threads, as it multiplexes HTTP connections on the Netty event loop.

## 3. Database Scaling
- **Database-per-Service:** The system correctly splits data across `incidents_db` and `alerts_db`, preventing a single monolithic database from bottlenecking.
- **PostgreSQL JSONB:** The `spans` table utilizes `JSONB` for `attributes` and `events`. While flexible, this can cause significant database bloat and index performance degradation if large text payloads are dumped into attributes over time.

## 4. Horizontal Scalability Constraints
- **State:** The backend services are largely stateless (relying on PostgreSQL and Redis), meaning `alert-service`, `trace-service`, and `ai-analytics-service` can be horizontally scaled safely by simply increasing the container replica count.
- **WebSocket Affinity:** The `bff-service` holds active WebSocket connections. Since it uses Redis Pub/Sub (`RedisMessageRelay.java`) to broadcast events, it can be horizontally scaled safely without sticky sessions. Every BFF replica receives the Redis event and forwards it to whichever clients happen to be connected to that specific replica.
