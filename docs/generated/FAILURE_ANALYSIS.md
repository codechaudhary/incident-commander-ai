# FAILURE ANALYSIS

This document details how the microservice architecture handles partial network failures, timeouts, and downstream unreachability.

## 1. External LLM Reliability (Anthropic API)
- **No Circuit Breaking:** The `ai-analytics-service` relies entirely on Anthropic's availability. There are no circuit breakers installed to prevent cascading failures or to stop sending traffic if Anthropic returns 500s.
- **No Rate Limiting:** If a massive incident occurs, generating thousands of traces, the `aiokafka` consumer will rapidly consume them and blast the Anthropic API concurrently, almost certainly resulting in HTTP 429 Too Many Requests.
- **Timeout Handling:** Standard HTTPX timeouts apply, but there is no exponential backoff or retry mechanism implemented in `LLMService.py`. If Anthropic drops the connection, the analysis is simply marked as `FAILED`.

## 2. BFF Gateway Fallbacks
- **No Resilience4j:** Despite the `README.md` explicitly claiming "Circuit Breakers (Resilience4j) on the BFF layer to protect against AI service timeouts," a scan of the `pom.xml` and `AiClient.java` proves this is false.
- **Fail-Silent Fallback:** When a downstream service (Trace, Alert, AI) is down or times out, the BFF's `WebClient` catches the exception using `onErrorResume(e -> Mono.empty())`. The dashboard will silently render `0` for metrics like "Open Alerts" or "Critical Alerts" instead of explicitly showing an error state or "N/A", which could falsely reassure an engineer during an outage.

## 3. Kafka Consumer Resiliency
- **Idempotency:** The `alert-service` implements strict database uniqueness on the `trace_id` column. If Kafka replays an event (at-least-once delivery), the `DuplicateAlertException` is cleanly caught and swallowed, preventing duplicate alerts.
- **Dead Letter Queues (DLQ):** Missing. If the `alert-service` encounters a malformed JSON payload that cannot be deserialized, it will either enter an infinite retry loop or drop the message depending on the default Spring Kafka error handler. There is no explicit DLQ configured.

## 4. Redis Ephemerality
- WebSockets rely on Redis Pub/Sub. If a message is published while the WebSocket connection is flapping, the message is permanently lost. The UI depends on a manual reload or REST polling to synchronize state.
