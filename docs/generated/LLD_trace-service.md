# LOW LEVEL DESIGN: trace-service

## 1. Class Architecture & Component Interaction

### Controllers
- **`TraceController.java`**
  - `@PostMapping("/api/v1/traces")`: Ingests `OtlpTraceRequest`.
  - `@GetMapping`: Handlers for listing and detailing traces. Uses `@PageableDefault`.

### Core Services
- **`TraceIngestionServiceImpl.java`**
  - Validates `traceId`.
  - Parses OTLP structured DTOs (`ScopeSpanDto`, `ResourceSpanDto`) into `TraceEntity` and `SpanEntity`.
  - Determines `TraceStatus` (ERROR, TIMEOUT, SUCCESS) by evaluating the worst-case scenario among child spans.
  - Generates `TracePersistedApplicationEvent`.
- **`TraceQueryServiceImpl.java`**
  - Read-only operations wrapper over `TraceRepository`. Maps entities back to `TraceSummaryResponse` using `TraceMapper`.

### Repositories
- **`TraceRepository.java`** (`JpaRepository<TraceEntity, UUID>`)
  - Contains derived query methods like `findByTraceId`.
- **`SpanRepository.java`** (`JpaRepository<SpanEntity, UUID>`)
  - Contains `findByTraceIdOrderByStartedAtAsc`.

### Events & Kafka
- **`TracePersistedEventListener.java`**
  - Uses `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` to ensure Kafka messages are only sent if the database transaction succeeds.
- **`KafkaTraceEventProducer.java`**
  - Wraps `KafkaTemplate` to serialize and publish `TraceIngestedEvent`.
