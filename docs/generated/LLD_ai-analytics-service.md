# LOW LEVEL DESIGN: ai-analytics-service

## 1. Class Architecture & Component Interaction

### API Layer
- **`analysis.py` (FastAPI Router)**
  - `@router.post("/trigger")`: Handles `TriggerAnalysisRequest`. Delegates to `AnalysisService.trigger` and triggers a FastAPI `BackgroundTasks` to execute `AnalysisService.process_from_trigger` asynchronously, returning `202 Accepted` immediately.
  - `@router.get("/{traceId}")`: Retrieves the analysis status or result via `AnalysisService.get_by_trace_id`.

### Core Services
- **`AnalysisService.py`**
  - Manages the end-to-end lifecycle of an analysis task.
  - Updates Redis state (PENDING -> PROCESSING -> COMPLETED/FAILED).
  - Interacts with `LLMService` to obtain the narrative.
  - Publishes the final `AnalysisDto` via `AnalysisPublisher`.
- **`LLMService.py`**
  - Abstracts interactions with Anthropic API.
  - Constructs system prompts and formats `TraceEventPayload` using `root_cause_prompt.py`.
  - Uses `httpx.AsyncClient` to send requests and awaits responses.
  - Maps Anthropic's JSON response to the `LLMAnalysisResult` Pydantic model.

### Data Access Layer (Redis)
- **`redis_repository.py`**
  - `RedisAnalysisStore`: Connection manager.
  - `RedisAnalysisRepository`: Implements data operations (`create_pending`, `mark_completed`, `get_by_trace_id`) utilizing `redis.asyncio` Client. Keys are serialized via JSON strings.

### Asynchronous Event Handling
- **`consumer.py`**
  - Runs `aiokafka.AIOKafkaConsumer` in a dedicated asyncio task loop.
  - Deserializes `KafkaEnvelope` and extracts `TraceEventPayload`.
  - Calls `AnalysisService.process_trace_event` to execute the LLM task.
- **`publisher.py`**
  - `AnalysisPublisher` uses Redis `PUBLISH` command to broadcast the `AnalysisDto` to the `analysis:live` channel.
