# LOW LEVEL DESIGN: alert-service

## 1. Class Architecture & Component Interaction

### Controllers
- **`AlertController.java`**
  - `@GetMapping`: List paginated alerts or fetch by `alertId`.
  - `@PatchMapping("/{alertId}/status")`: Update alert status. Validates payload before passing to service.

### Core Services
- **`AlertCreationService.java`**
  - Parses incoming `TraceIngestedPayload`.
  - Maps `FailureType` to `Severity` (e.g., `CASCADING_FAILURE` -> `CRITICAL`).
  - Constructs `AlertEntity` and saves via repository.
  - Generates `AlertCreatedEvent`.
- **`AlertQueryService.java`** & **`AlertUpdateService.java`**
  - Domain separation. Update service changes state, sets `updatedAt`, and triggers Kafka publish.

### Repositories
- **`AlertRepository.java`** (`JpaRepository<AlertEntity, UUID>`)
  - Uses specific queries: `findByAlertId`, `findByTraceId`.

### Events & Kafka
- **`TraceEventConsumer.java`**
  - `@KafkaListener` targeting `alert.kafka.topics.trace-ingested`.
  - Filters out traces where `status == SUCCESS`.
  - Catches `DuplicateAlertException` to ensure idempotent processing per `traceId`.
- **`AlertEventProducer.java`**
  - Uses `KafkaTemplate` to emit events like alert creation or status changes.
