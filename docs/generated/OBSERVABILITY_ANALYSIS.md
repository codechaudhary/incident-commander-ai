# OBSERVABILITY ANALYSIS

This document evaluates the instrumentation, monitoring, and tracing setup within the Incident Commander AI codebase.

## 1. Metrics & Health Checks
- **Spring Boot Actuator:** Present in the `pom.xml` files for `bff-service`, `trace-service`, `alert-service`, and `order-simulator`. Exposes basic `/actuator/health` endpoints.
- **Micrometer:** `micrometer-observation` is pulled in transitively via the Spring Boot parent pom.
- **Python FastApi Health:** The `ai-analytics-service` has a basic `/health` JSON endpoint.
> [!WARNING]
> **Missing Exporters:** While Actuator and Micrometer are present, there is no `micrometer-registry-prometheus` dependency. The system does not expose a `/actuator/prometheus` scrape endpoint, meaning the metrics cannot currently be ingested by standard Prometheus/Grafana stacks.

## 2. Distributed Tracing
> [!CAUTION]
> **IRONIC TRACING FAILURE DETECTED**
> The entire purpose of this project is to analyze OpenTelemetry distributed traces. However, **the microservices themselves are not instrumented.** 
> There is no OpenTelemetry javaagent, no `micrometer-tracing-bridge-otel`, and no Zipkin/Jaeger exporters in any of the services. If the `trace-service` itself crashes, there is no trace data generated to debug it. 

## 3. Logging
- **Java Services:** Uses standard Logback (via Spring Boot default `slf4j`). Logs are written to stdout. There is no structured JSON logging configured (e.g., `logstash-logback-encoder`), making it difficult to parse logs in ELK/Splunk.
- **Python Service:** Uses `structlog` for structured JSON logging. This is a best practice, but it is inconsistent with the Java services.
- **Log Aggregation:** There is no Fluentd, Logstash, or Promtail container in the `docker-compose.yml`. Logs remain isolated per container.

## 4. External Monitoring
The `README.md` mentions "Azure App Insights", but there is no `applicationinsights-spring-boot-starter` dependency or Python `azure-monitor-opentelemetry` SDK installed. This claim is completely unsubstantiated by the code.
