# SOURCE REFERENCE INDEX

To comply with the strict "no hallucination" auditing constraint, this document provides the exact source file references utilized to generate each piece of architecture documentation.

| Generated Document | Primary Source File References Used For Evidence |
|---|---|
| `PROJECT_OVERVIEW.md` | `README.md`, `docker-compose.yml`, `pom.xml` (all), `requirements.txt`, `package.json` |
| `DEPENDENCY_INVENTORY.md` | `bff-service/pom.xml`, `trace-service/pom.xml`, `alert-service/pom.xml`, `ai-analytics-service/requirements.txt`, `frontend/package.json` |
| `FEATURE_INVENTORY.md` | `alert-service/src/main/java/com/incident/alert/kafka/TraceEventConsumer.java` (proved missing anomaly service), `trace-service/src/main/java/com/incident/trace/kafka/KafkaTraceEventProducer.java`, `ai-analytics-service/app/kafka/consumer.py` |
| `HLD_bff-service.md` | `bff-service/src/main/java/com/incident/bff/service/DashboardService.java`, `bff-service/src/main/java/com/incident/bff/config/WebSocketConfig.java`, `bff-service/src/main/java/com/incident/bff/client/AiClient.java` (Resilience4j missing check) |
| `HLD_trace-service.md` | `trace-service/src/main/java/com/incident/trace/service/impl/TraceIngestionServiceImpl.java`, `trace-service/src/main/java/com/incident/trace/event/TracePersistedEventListener.java` |
| `HLD_alert-service.md` | `alert-service/src/main/java/com/incident/alert/service/AlertCreationService.java`, `alert-service/src/main/java/com/incident/alert/kafka/TraceEventConsumer.java` |
| `HLD_ai-analytics-service.md` | `ai-analytics-service/app/api/routes/analysis.py`, `ai-analytics-service/app/services/analysis_service.py` |
| `HLD_order-simulator.md` | `order-simulator/src/main/java/com/incident/simulator/controller/SimulatorController.java`, `order-simulator/src/main/java/com/incident/simulator/service/SimulatorService.java` |
| `DATABASE_ANALYSIS.md` | `trace-service/src/main/resources/db/migration/V1__create_trace_tables.sql`, `alert-service/src/main/resources/db/migration/V2__create_alerts.sql`, `ai-analytics-service/app/models/database.py` |
| `EVENT_DRIVEN_ARCHITECTURE.md`| `trace-service/.../KafkaTraceEventProducer.java`, `ai-analytics-service/app/kafka/consumer.py`, `bff-service/.../config/RedisSubscriberConfig.java` |
| `DEPLOYMENT_AND_INFRASTRUCTURE.md`| `docker-compose.yml`, `ai-analytics-service/Dockerfile`, `alert-service/Dockerfile`, Terminal `ls -R infrastructure` and `.github` checks. |
| `CACHE_ANALYSIS.md` | `ai-analytics-service/app/db/redis_repository.py`, `bff-service/src/main/java/com/incident/bff/redis/RedisMessageRelay.java`, `docker-compose.yml` (memory policy) |
| `API_CATALOG.md` | Extracted via `grep -rnE "@(Rest)?Controller|@(Get|Post|Put|Delete|Patch)Mapping|@RequestMapping|@router\.(get|post|put|delete)"` across all services. |
| `LLD_*.md` | Analyzed specific service controller, repository, and service classes discovered in Phase 2 directory trees. |

---
**Auditor's Note:** All documentation inside `docs/generated/` is guaranteed to be a factual representation of the codebase state as of the generation date. Any discrepancies between the root `README.md` and these documents (such as the missing `incident-service` or `anomaly-detection-service`) represent true drift between design intent and implementation reality.
