# HIGH LEVEL DESIGN: ai-analytics-service

## 1. Purpose & Business Responsibility
The `ai-analytics-service` is the intelligence engine of the platform. It consumes trace events, translates complex span trees into contextual prompts, and calls an external Large Language Model (Anthropic Claude) to perform root-cause analysis and generate actionable postmortem narratives.

## 2. Architecture Position
- **Layer:** Core Domain Service (AI & Analytics).
- **Tech Stack:** Python 3.9+, FastAPI, SQLAlchemy, aiokafka, Pydantic, HTTPX.

## 3. Dependencies
### Inbound Dependencies
- **Kafka:** Asynchronously consumes `trace.ingested` events to automatically trigger analysis.
- **REST:** Receives explicit HTTP `POST /analyses/trigger` requests from the BFF to force a new analysis, and `GET /analyses/{id}` to fetch results.

### Outbound Dependencies
- **Anthropic Claude API:** External LLM API accessed via asynchronous HTTP calls (`httpx`).
- **PostgreSQL:** Persists generated narratives and analysis status (`AnalysisEntity`).
- **Redis Pub/Sub:** Publishes `narration.ready` events upon analysis completion so the BFF can push them to the frontend via WebSockets.

## 4. Architecture & Request Flow
1. **Trigger:** Analysis can be triggered either via Kafka event (`AIOKafkaConsumer`) or a direct REST call (`/analyses/trigger`).
2. **State Management:** A pending analysis record is immediately created in the database to prevent duplicate parallel processing.
3. **Prompt Generation:** The service parses the trace and all associated spans, extracting duration, failure types, and error messages to build a highly structured prompt using `root_cause_prompt.py`.
4. **LLM Inference:** An asynchronous call is made to the Anthropic API.
5. **Response Parsing:** The LLM's response is validated and coerced into structured data using Pydantic.
6. **Completion:** The database is updated, and the final narrative payload is published to the `analysis:live` Redis channel.

## 5. Reliability & Scalability
- **Asynchronous Execution:** Built natively with Python's `asyncio`. When a REST trigger is received, the HTTP response returns immediately (HTTP 202 Accepted) while the LLM call proceeds in a non-blocking background task.
- **Stateless Analysis:** Analysis tasks do not require in-memory state across requests, making the service horizontally scalable.
- **LLM Rate Limiting Risk:** There is currently no observable token-bucket or queue-based rate limiter guarding the external LLM calls. A massive spike in errors could result in HTTP 429 Too Many Requests from Anthropic.
