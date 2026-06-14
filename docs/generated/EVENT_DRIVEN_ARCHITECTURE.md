# EVENT-DRIVEN ARCHITECTURE

This document maps all asynchronous messaging patterns, Kafka topics, Redis Pub/Sub channels, event schemas, and handling mechanisms implemented in the codebase.

## 1. Apache Kafka Flow
Kafka is used for durable, replayable, service-to-service domain events.

### Topic: `trace-ingested-events`
* **Producer:** `trace-service` (`KafkaTraceEventProducer.java`).
* **Trigger:** Emitted immediately after a root trace and its spans are persisted to PostgreSQL.
* **Partition Key:** `traceId`.
* **Payload Structure (`TraceIngestedPayload`):**
  ```json
  {
    "traceId": "string",
    "rootService": "string",
    "status": "string (ERROR|TIMEOUT|SUCCESS)",
    "failureType": "string",
    "durationMs": 1000
  }
  ```
* **Consumer 1 (`alert-service`):**
  * `TraceEventConsumer.java` consumes this topic. 
  * Checks if `status` is `ERROR` or `TIMEOUT`.
  * If true, creates an alert. Throws and swallows `DuplicateAlertException` to ensure idempotency.
  * **Config:** Concurrency = 3 threads per pod.
* **Consumer 2 (`ai-analytics-service`):**
  * `consumer.py` uses `aiokafka.AIOKafkaConsumer`.
  * Triggered upon receiving the event to begin async LLM root-cause analysis.
  * **Config:** Listens to `KAFKA_TRACE_TOPIC` environment variable.

### Topic: `alert.events`
* **Producer:** `alert-service` (`AlertEventProducer.java`).
* **Trigger:** Emitted when an alert is created (`AlertCreatedEvent`), acknowledged, or resolved.
* **Partition Key:** `traceId`.
* **Consumer:** **NOT FOUND IN CODEBASE.** No service currently consumes this topic. It appears to be implemented for future downstream extensibility.

### Kafka Reliability Mechanisms
* **Retry Strategy / DLQ:** **NOT FOUND IN CODEBASE.** Standard Spring Kafka `@KafkaListener` defaults apply (in-memory retries). No Dead Letter Queue (DLQ) configuration (`DeadLetterPublishingRecoverer`) is explicitly configured in `KafkaProducerConfig` or the consumer configs.
* **Ordering:** Events for the same trace are placed on the same partition via the `traceId` key, guaranteeing chronological processing per trace.

---

## 2. Redis Pub/Sub Flow
Redis Pub/Sub is used for ephemeral, high-throughput, "fire-and-forget" broadcasting to WebSockets. Durability is not required here because the UI will fetch latest state via REST on reload if it misses a message.

### Channels
1. **`traces:live`**
   * **Producer:** Unknown/Missing in code. (Likely intended from trace-service but no Redis publisher exists in `trace-service`).
   * **Relay:** `bff-service` routes to STOMP `/topic/traces`.
2. **`alerts:live`**
   * **Producer:** `alert-service`. (Intended to broadcast alert updates).
   * **Relay:** `bff-service` routes to STOMP `/topic/alerts`.
3. **`analysis:live`**
   * **Producer:** `ai-analytics-service` (`redis_repository.py`). Broadcasts the final generated LLM narrative.
   * **Relay:** `bff-service` routes to STOMP `/topic/analysis`.

### Relay Mechanism
The `bff-service` utilizes `ReactiveRedisMessageListenerContainer` (`RedisSubscriberConfig.java`) to subscribe to the above channels. When a message arrives, `RedisMessageRelay.java` pushes the exact JSON payload down the active WebSocket connections using `SimpMessagingTemplate`.

## Architectural Discrepancies
> [!WARNING]
> **Missing Topics from README:**
> The `README.md` documents topics named `anomaly.detected` and `narration.ready`.
> **Reality:** 
> 1. `anomaly.detected` does not exist and is never produced or consumed.
> 2. `narration.ready` is actually broadcast via **Redis Pub/Sub** (`analysis:live`), not Kafka as implied by the README diagram.
