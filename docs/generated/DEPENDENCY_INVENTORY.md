# DEPENDENCY INVENTORY

This document provides a complete inventory of all databases, external tools, messaging systems, and third-party libraries across the codebase based strictly on `pom.xml`, `requirements.txt`, `package.json`, and `docker-compose.yml`.

## Infrastructure Dependencies

| Dependency | Version | Purpose | Usage Location | Authentication |
|---|---|---|---|---|
| **PostgreSQL** | 16 | Primary relational database | `docker-compose.yml` (`incident-postgres`) | Username/Password (`POSTGRES_USER`) |
| **Apache Kafka** | latest | Event broker for pub/sub | `docker-compose.yml` (`incident-kafka`) | PLAINTEXT (Local) |
| **Redis** | 7-alpine | Caching and WebSocket pub-sub | `docker-compose.yml` (`incident-redis`) | None (Local) |
| **Kafka-UI** | latest | Local Kafka observability | `docker-compose.yml` (`kafka-ui`) | None |

## Backend Dependencies (Java)
Used across `bff-service`, `trace-service`, `alert-service`, and `order-simulator`.

| Dependency | Purpose | Caller Service(s) |
|---|---|---|
| **Spring Boot (3.2.x)** | Core application framework | All Java services |
| **Spring WebFlux** | Reactive, non-blocking HTTP server and web client | `bff-service` |
| **Spring WebMVC** | Blocking HTTP server (Tomcat) | `trace-service`, `alert-service`, `order-simulator` |
| **Spring Data JPA** | ORM for PostgreSQL | `trace-service`, `alert-service` |
| **Spring Kafka** | Kafka producers and consumers | All Java services |
| **Spring Data Redis (Reactive/Blocking)** | Redis caching and pub-sub relay | `bff-service` |
| **Spring WebSocket** | WebSocket implementation for real-time frontend updates | `bff-service` |
| **Flyway** | Database schema migrations | `trace-service`, `alert-service` |
| **Resilience4j (Implied/Planned)** | Circuit breakers | `bff-service` (DESIGN INTENT DETECTED BUT IMPLEMENTATION NOT FOUND in `pom.xml` explicit imports, needs deeper check) |
| **Springdoc OpenAPI UI** | Swagger documentation | All Java services |
| **Lombok** | Boilerplate reduction | All Java services |
| **Hypersistence Utils** | Advanced Hibernate types (JSONB) | `trace-service`, `alert-service` |

## Backend Dependencies (Python)
Used in `ai-analytics-service`.

| Dependency | Version | Purpose |
|---|---|---|
| **FastAPI** | 0.115.6 | Async web framework |
| **Uvicorn** | 0.34.0 | ASGI web server |
| **SQLAlchemy** | 2.0.50 | ORM for database access |
| **Asyncpg** | 0.31.0 | Async PostgreSQL driver |
| **Aiokafka** | 0.14.0 | Async Kafka producer/consumer |
| **Redis** | 5.2.1 | Redis client |
| **HTTPX** | 0.28.1 | Async HTTP client (likely for LLM APIs) |
| **Pydantic** | 2.13.4 | Schema validation |
| **Structlog** | 24.4.0 | Structured JSON logging |

## Frontend Dependencies (Node.js/React)
Used in `frontend`.

| Dependency | Version | Purpose |
|---|---|---|
| **Next.js** | 14.2.3 | React framework |
| **React** | 18 | UI Library |
| **TailwindCSS** | 3.4.1 | Utility styling |
| **Axios** | 1.17.0 | HTTP Client |
| **TanStack React Query** | 5.101.0 | Server-state management |
| **STOMP.js** | 7.3.0 | WebSocket client for Spring WebSocket |
| **Framer Motion** | 12.40.0 | Animations |
| **Lucide React** | 1.18.0 | Iconography |

## Internal API Dependencies (Service-to-Service)
* `bff-service` acts as an API Gateway and depends on REST APIs of `trace-service`, `alert-service`, and `ai-analytics-service`.
* `ai-analytics-service` depends on `trace-service` (presumably fetching full trace data for analysis).
* `order-simulator` depends on `trace-service` (injecting fake spans).

## Missing/Unverified Dependencies
> **NOT FOUND IN CODEBASE:** The `README.md` mentions Anthropic Claude via "Spring AI" in the Java `incident-service`. However, `incident-service` does not exist in the repository, and `spring-ai` is absent from all `pom.xml` files. The Anthropic integration appears to exist only via raw HTTPX calls in the Python `ai-analytics-service`.
> **NOT FOUND IN CODEBASE:** `pgvector` was mentioned for RAG, but there is no `rag-service` in the repository, nor any vector dependencies in `requirements.txt`.
