# MY CONTRIBUTION GUIDE

This document serves as a strategic roadmap for developers looking to contribute, extend, or fix the Incident Commander AI architecture. 

## 1. How to Add a New Downstream Event Consumer
If you want to build a new service (e.g., a `notification-service` to send Slack alerts):
1. **Listen to Kafka, not Redis:** Bind an `@KafkaListener` to the `alert.events` topic (currently unutilized in the codebase).
2. **Idempotency is Required:** The Kafka topics use at-least-once delivery. You must build deduplication logic (e.g., using `trace_id` + `alert_id` as a composite primary key) in your consumer to ensure you don't send multiple Slack messages for the same incident.

## 2. How to Expose New Real-time Dashboard Metrics
If you want to add a new widget to the React frontend (e.g., "Active Users Impacted"):
1. **Backend Aggregation:** Update the `bff-service/src/main/java/com/incident/bff/service/DashboardService.java`.
2. **Expand the `Mono.zip`:** Add a new `Mono` representing your downstream API call, and add it to the `Mono.zip` list. Ensure you use `.onErrorResume(e -> Mono.just(0))` to prevent the entire dashboard from failing if your new metric API is offline.
3. **Frontend Sync:** Update `DashboardSummaryResponse.java` to include the new field, and modify the `Zustand` store in the frontend to capture it.

## 3. High-Priority Fixes (Low Hanging Fruit)
If you want to make an immediate, high-impact contribution:
- **Implement Resilience4j on BFF:** Wrap the `AiClient.java` WebClient calls with `@CircuitBreaker`.
- **Add Tests to Trace Service:** Write `TraceIngestionServiceImplTest.java` using JUnit and Mockito to verify the span tree parsing logic.
- **Remove Wildcard CORS:** Update `CorsConfig.java` in the `bff-service` to only accept requests from your exact frontend domain (e.g., `http://localhost:3000`).

## 4. How to Update LLM Prompts
- The LLM logic lives entirely in `ai-analytics-service/app/prompts/root_cause_prompt.py`.
- If you add new fields to the `TraceEventPayload`, you must ensure they are injected into the f-string prompt context block in `LLMService.analyze()`.
