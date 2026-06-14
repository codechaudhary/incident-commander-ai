# HIGH LEVEL DESIGN: order-simulator

## 1. Purpose & Business Responsibility
The `order-simulator` is a non-production utility service designed strictly for demonstration and testing purposes. It generates synthetic trace payloads representing successful operations, slow operations, and cascading failures, mimicking a real microservice environment.

## 2. Architecture Position
- **Layer:** External Client / Load Generator.
- **Tech Stack:** Java 17, Spring Boot 3.2.x (WebMVC).

## 3. Dependencies
### Outbound Dependencies
- `trace-service`: The simulator uses a `TraceClient` to `POST` synthetic traces to the ingestion endpoint.

## 4. Architecture Flow
1. Receives an HTTP POST request to `/api/v1/simulate` with a requested `FailureType` (e.g., NONE, TIMEOUT, CASCADING_FAILURE).
2. Uses `TraceGenerator` logic to construct a realistic OpenTelemetry-like span tree.
3. Submits the payload to the `trace-service` via a blocking HTTP call.

## 5. Security & Deployment Notes
> [!WARNING]
> Because this service injects fabricated data directly into the production trace ingestion pipeline, **it should never be deployed to a production environment** or exposed to the public internet. It lacks authentication and represents a vector for database exhaustion if abused.
