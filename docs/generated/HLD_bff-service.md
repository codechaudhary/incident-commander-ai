# HIGH LEVEL DESIGN: bff-service

## 1. Purpose & Business Responsibility
The `bff-service` (Backend for Frontend) acts as the primary API Gateway and real-time event router for the Incident Commander AI dashboard. It is responsible for orchestrating requests across downstream microservices (Trace, Alert, AI) to aggregate data into a unified dashboard view. It also manages WebSocket connections to push real-time updates to connected clients.

## 2. Architecture Position
- **Layer:** Edge / Gateway Layer.
- **Role:** Aggregator and WebSocket server.
- **Tech Stack:** Java 17, Spring Boot 3.2.x, Spring WebFlux (Reactor), Spring WebSocket.

## 3. Dependencies
### Inbound Dependencies
- **Upstream clients:** React SPA dashboard (Frontend) via HTTP/REST and WebSockets (`STOMP.js`).

### Outbound Dependencies (Downstream Services)
- `trace-service`: Fetches trace summaries, total counts, and error counts.
- `alert-service`: Fetches alert states (open, critical).
- `ai-analytics-service`: Triggers root cause analysis and fetches analysis status.

### Infrastructure Dependencies
- **Redis (Reactive):** Uses Redis Pub/Sub channels (`alerts:live`, `analysis:live`, `traces:live`) to receive events from other services and broadcast them.

## 4. Communication Methods & Protocols
- **Inbound:** REST (JSON over HTTP/1.1), WebSockets (STOMP over WS).
- **Outbound:** REST WebClient (JSON over HTTP/1.1) to downstream services.
- **Pub/Sub:** Redis subscribe (`RedisMessageRelay.java`).

## 5. Resilience & Fallbacks
> [!WARNING]
> **NOT FOUND IN CODEBASE:** The `README.md` explicitly states that Resilience4j circuit breakers are used to protect against AI service outages. A scan of the `bff-service` code (`AiClient.java`, `pom.xml`) reveals **no Resilience4j annotations or dependencies**. 
> **Actual Fallback Implementation:** The `AiClient` simply uses Reactor's `onErrorResume` to catch exceptions and return `Mono.empty()`. There is no circuit breaker state machine (Open/Half-Open/Closed) implemented.

## 6. Real-Time Architecture (WebSockets)
1. Spring WebSocket is configured via `WebSocketConfig.java` on the `/ws` endpoint with SockJS fallback.
2. The `RedisSubscriberConfig.java` listens to specific Redis channels.
3. The `RedisMessageRelay.java` maps Redis channels to STOMP destinations (e.g., `alerts:live` -> `/topic/alerts`) and broadcasts payloads using `SimpMessagingTemplate.convertAndSend()`.

## 7. Scalability & Scalability Characteristics
- **WebFlux Non-Blocking I/O:** The `DashboardService.java` uses `Mono.zip()` to execute 6 parallel outbound HTTP requests (traces, error traces, alerts, critical alerts, analyses, avg duration) simultaneously on the Netty event loop, vastly reducing wall-clock response time and thread consumption.
- **Statelessness:** The service is stateless regarding HTTP requests. WebSocket sessions are sticky or handled via Redis Pub/Sub, allowing multiple BFF instances to run behind a load balancer and broadcast Redis events to their locally connected WS clients.
