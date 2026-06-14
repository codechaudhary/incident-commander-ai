# TRACEABILITY MATRIX

This document traces the business requirements and core features described in the initial architectural vision down to the specific classes and methods that implement them.

| Business Requirement / Feature | Implementing Service | Key Code Components | Verification Status |
|---|---|---|---|
| **Trace Ingestion** | `trace-service` | `TraceController.java` (`POST /api/v1/traces`)<br>`TraceIngestionServiceImpl.java` | ✅ IMPLEMENTED |
| **Span Hierarchy Persistence** | `trace-service` | `TraceRepository.java`, `SpanRepository.java` (using PostgreSQL JSONB) | ✅ IMPLEMENTED |
| **Real-time Event Emission** | `trace-service` | `KafkaTraceEventProducer.java`, `TracePersistedEventListener.java` | ✅ IMPLEMENTED |
| **Rule-based Alerting** | `alert-service` | `TraceEventConsumer.java`, `AlertCreationService.java` | ✅ IMPLEMENTED |
| **Alert State Management** | `alert-service` | `AlertController.java` (`PATCH /api/v1/alerts/{id}/status`) | ✅ IMPLEMENTED |
| **LLM Root Cause Analysis** | `ai-analytics-service` | `analysis_service.py`, `llm_service.py`, `root_cause_prompt.py` | ✅ IMPLEMENTED |
| **Async LLM Triggering** | `ai-analytics-service` | `consumer.py` (`AIOKafkaConsumer`) | ✅ IMPLEMENTED |
| **Unified Dashboard API** | `bff-service` | `DashboardService.java` (`Mono.zip` fan-out to 6 REST calls) | ✅ IMPLEMENTED |
| **WebSocket Real-time Updates**| `bff-service` | `RedisSubscriberConfig.java`, `RedisMessageRelay.java` | ✅ IMPLEMENTED |
| **Synthetic Load Generation** | `order-simulator` | `TraceGenerator.java`, `SimulatorService.java` | ✅ IMPLEMENTED |
| **JWT Authentication** | *Missing* (`auth-service`) | *None found in code* | ❌ MISSING |
| **Vector DB / RAG Search** | *Missing* (`rag-service`) | *None found in code* | ❌ MISSING |
| **Statistical Anomaly Detection**| *Missing* (`anomaly-service`) | *None found in code* | ❌ MISSING |
| **API Rate Limiting & Resilience**| *Missing* (Resilience4j) | *None found in code* | ❌ MISSING |
| **CI/CD & Cloud Infrastructure** | *Missing* (Azure Bicep) | *None found in code* | ❌ MISSING |
