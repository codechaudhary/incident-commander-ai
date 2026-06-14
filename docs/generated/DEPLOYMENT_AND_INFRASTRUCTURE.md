# DEPLOYMENT AND INFRASTRUCTURE

This document analyzes the infrastructure-as-code, containerization, and CI/CD pipelines defined in the repository.

## 1. Local Orchestration (Docker Compose)
The project utilizes a comprehensive `docker-compose.yml` for local development.

### Managed Containers
* `postgres`: PostgreSQL 16 server exposing port `5432` with a persistent volume (`postgres_data`).
* `kafka`: Apache Kafka running in KRaft mode (no Zookeeper) on port `9092`.
* `kafka-ui`: Web UI for Kafka observability on port `8085`.
* `redis`: Redis 7-alpine exposing port `6379`. Configured with `--maxmemory 128mb --maxmemory-policy allkeys-lru`.
* **Microservices:** Strangely, the application services (`bff-service`, `trace-service`, etc.) are **NOT** defined in the `docker-compose.yml`. The compose file only spins up the infrastructure backing services. To run the full stack locally, developers must run the services manually or use an undocumented `docker-compose.prod.yml` (mentioned in README but missing from the repo).

## 2. Containerization (Dockerfiles)
Dockerfiles exist for some services but not all.

* **`ai-analytics-service/Dockerfile`**:
  * Base Image: `python:3.14-slim` (Note: Python 3.14 does not exist yet; this is an error/future-proofing artifact).
  * Runs as a non-root user (`app`).
  * Exposes port `8090`.
* **`alert-service/Dockerfile`**: Present in the directory structure.
* **Missing Dockerfiles:** `bff-service`, `trace-service`, `order-simulator`, and `frontend` do not have Dockerfiles in their root directories according to the directory scan.

## 3. Cloud Infrastructure & CI/CD (Azure)
> [!WARNING]
> **INFRASTRUCTURE GAPS DETECTED**
> The `README.md` explicitly describes a robust Azure deployment model including:
> 1. `infrastructure/azure/container-apps.bicep`
> 2. `infrastructure/azure/app-insights.bicep`
> 3. `.github/workflows/deploy.yml`
> 
> **Finding:** None of these files or directories exist in the repository. The project has no implemented CI/CD pipeline, no Azure Bicep/Terraform templates, and no cloud-native deployment manifests (e.g., Kubernetes YAML or Helm charts).

## 4. Configuration Management
Services are configured using environment variables. The `.env.example` outlines:
* `ANTHROPIC_API_KEY`
* `TRACES_DB_URL`, `INCIDENTS_DB_URL`, `ALERTS_DB_URL`, `AI_DB_URL`
* `REDIS_URL`, `KAFKA_BOOTSTRAP_SERVERS`
* `JWT_SECRET` (Unused in code)

**Secrets Management:** 
Secrets are passed as plain environment variables. There is no integration with Azure Key Vault, AWS Secrets Manager, or HashiCorp Vault.
