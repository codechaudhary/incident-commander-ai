# DESIGN DECISIONS

This document logs the inferred architectural choices made by the original development team, explaining the "why" behind the code based on standard software engineering patterns.

## 1. Database-per-Service
**Decision:** The `trace-service` and `alert-service` have entirely separate PostgreSQL schemas (`incidents_db` vs `alerts_db`) with independent Flyway migrations.
**Tradeoff:** 
- *Pros:* Decouples services, allowing them to scale database connections independently. Prevents tight coupling where `alert-service` writes directly to `trace` tables.
- *Cons:* No cross-database joins. The `alert-service` must store `trace_id` as a soft reference, and the `bff-service` must stitch the data together in memory via API calls.

## 2. Polyglot Microservices
**Decision:** Core event ingestion and aggregation are built in Java (Spring Boot), while AI analysis is built in Python (FastAPI).
**Tradeoff:**
- *Pros:* Python has a vastly superior ecosystem for interacting with LLMs (e.g., Anthropic SDKs, Prompt tooling). Java/Spring Boot is highly mature for high-throughput HTTP REST APIs and Kafka consumer groups.
- *Cons:* Operational complexity. The team must maintain CI/CD pipelines, Dockerfiles, and observability tooling for two entirely different stacks.

## 3. Kafka vs. Redis Pub/Sub
**Decision:** Kafka is used for `trace.ingested` events, while Redis Pub/Sub is used for `analysis:live` and `alerts:live`.
**Tradeoff:**
- *Pros:* Kafka provides guaranteed delivery, replayability, and consumer group scaling for critical backend domain logic (like saving an alert to a database). Redis Pub/Sub is extremely fast and lightweight, making it ideal for transient WebSocket payloads where durability doesn't matter (if the user's browser is closed, they don't need the event).
- *Cons:* Maintains two distinct messaging infrastructures, increasing cloud costs and operational overhead.

## 4. OTLP JSONB Storage
**Decision:** OpenTelemetry attributes and events are stored as raw `JSONB` in the `spans` PostgreSQL table.
**Tradeoff:**
- *Pros:* Complete schema flexibility. OpenTelemetry tags vary wildly between services (e.g., HTTP spans have `http.url`, Database spans have `db.statement`). `JSONB` prevents a schema explosion.
- *Cons:* Querying deep JSON arrays in Postgres is slower than querying indexed relational columns. If the system needed to search for all traces where `db.statement LIKE '%DROP%'`, performance could degrade at scale.
