# HIGH LEVEL DESIGN: trace-service

## 1. Purpose & Business Responsibility
The `trace-service` is responsible for accepting raw distributed trace payloads (span trees), persisting them, and initiating the incident analysis flow. It computes trace durations, identifies `ERROR` or `TIMEOUT` statuses within the span tree, and emits a Kafka event notifying downstream consumers that a new trace is ready for analysis and alerting.

## 2. Architecture Position
- **Layer:** Core Domain Service (Ingestion).
- **Role:** System of record for trace data; Event source for the architecture.
- **Tech Stack:** Java 17, Spring Boot 3.2.x (WebMVC), Spring Data JPA, Spring Kafka, Flyway.

## 3. Dependencies
### Inbound Dependencies
- `bff-service` (REST calls to fetch paginated trace summaries and trace details).
- `order-simulator` (REST POST to ingest simulated traces).

### Outbound Dependencies
- **Apache Kafka:** Emits `trace.ingested` events to trigger the AI analysis and Alert evaluations.
- **PostgreSQL:** Persists `TraceEntity` and `SpanEntity` via Spring Data JPA.

## 4. Communication Methods & Protocols
- **Inbound:** REST (HTTP/1.1) controllers (`TraceController.java`).
- **Outbound:** Kafka Producer API (`KafkaTraceEventProducer.java`).
- **Database:** JDBC / Hibernate Dialect.

## 5. Domain Ownership
The service strictly owns the `traces` and `spans` database tables. No other service is permitted to write or read these tables directly. It provides DTO mapping (`TraceMapper.java`) to shield its internal entity structure from consumers.

## 6. Architecture & Request Flow
1. **Ingestion:** HTTP `POST /api/v1/traces` receives a JSON trace payload.
2. **Validation:** Payload is validated for required fields. `DuplicateTraceException` is thrown if the trace ID already exists.
3. **Processing:** `TraceIngestionServiceImpl.java` parses the span tree. 
4. **Persistence:** Saves the root trace and child spans in a transactional block to PostgreSQL.
5. **Event Emission:** A Spring ApplicationEvent (`TracePersistedApplicationEvent`) is fired. The `TracePersistedEventListener` intercepts this and uses `KafkaTraceEventProducer` to publish the `TraceIngestedEvent` to the `alert.kafka.topics.trace-ingested` Kafka topic.

## 7. Reliability & Scalability
- **Database Migrations:** Schema integrity is enforced via Flyway migrations.
- **Synchronous Bottleneck Risk:** The ingestion endpoint is synchronous. It writes to the DB and emits to Kafka within the HTTP request boundary. Under extreme load (thousands of traces per second), DB write latency will block HTTP handler threads. An async ingestion queue (e.g., writing straight to Kafka and consuming locally to save to DB) would improve scalability.
- **JSONB Support:** Uses Hypersistence Utils to handle complex attributes as JSONB columns in Postgres, allowing flexible span attribute storage without strict relational schema changes.
