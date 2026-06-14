# HIGH LEVEL DESIGN: alert-service

## 1. Purpose & Business Responsibility
The `alert-service` evaluates system events and trace failures to generate and track operational alerts. It acts as the rules engine for the platform, turning raw error traces into actionable incident flags for engineers. It manages the lifecycle of an alert from `OPEN` to `ACKNOWLEDGED` to `RESOLVED`.

## 2. Architecture Position
- **Layer:** Core Domain Service (Event Processor & REST API).
- **Tech Stack:** Java 17, Spring Boot 3.2.x (WebMVC), Spring Data JPA, Spring Kafka, Flyway.

## 3. Dependencies
### Inbound Dependencies
- **Kafka:** Consumes `trace.ingested` events to evaluate incoming failures.
- **REST:** `bff-service` queries the REST API to fetch alert statuses for the dashboard.

### Outbound Dependencies
- **Kafka:** Emits `alert.events` (alert created, status changed) to a Kafka topic. These events are intended for historical tracking or potential downstream automation.
- **PostgreSQL:** Persists `AlertEntity` records.

## 4. Architectural Deviation from Documentation
> [!WARNING]
> **DOCUMENTATION MISMATCH DETECTED**
> The `README.md` explicitly states the `alert-service` listens to `anomaly.detected` events produced by an `anomaly-detection-service`. **Code evidence proves this is false.** 
> The `TraceEventConsumer.java` directly listens to the `trace-ingested-events` topic. It acts immediately on trace payloads where `status == "ERROR" || status == "TIMEOUT"`. There is no statistical z-score anomaly detection happening before an alert is fired.

## 5. Domain Ownership
The service strictly owns the `alerts` table. It controls the alert state machine (`AlertStatus`: OPEN, ACKNOWLEDGED, RESOLVED).

## 6. Architecture & Event Flow
1. **Event Consumption:** `TraceEventConsumer` listens to the Kafka topic.
2. **Evaluation:** Filters out non-error traces. Valid errors are passed to `AlertCreationService`.
3. **Deduplication:** A `DuplicateAlertException` is thrown and caught silently if an alert for the same `traceId` already exists, preventing alert spam for duplicate deliveries.
4. **Persistence:** `AlertEntity` is saved to PostgreSQL.
5. **Emission:** `AlertEventProducer` publishes an `AlertCreatedEvent` back to Kafka for the broader ecosystem.

## 7. Reliability & Scalability Characteristics
- **Consumer Concurrency:** Configured with `concurrency = "3"` on the `@KafkaListener` to allow parallel processing of incoming trace events if the topic is partitioned.
- **Idempotency:** Partially implemented via database constraint on `traceId` to throw `DuplicateAlertException`, which protects against Kafka at-least-once delivery duplicates.
