# AI Analytics Service

FastAPI implementation of the contract-defined `ai-analysis-service` for Incident Commander AI.

## What It Does

- Consumes `TRACE_INGESTED` messages from Kafka topic `trace-events`
- Skips successful traces and analyzes `ERROR` / `TIMEOUT` traces
- Persists rows in PostgreSQL table `ai_analysis`
- Calls OpenCode Zen with ordered model fallback and the frozen root-cause prompt contract
- Publishes completed analyses to Redis channel `analysis:live`
- Exposes `/health`, `/ready`, and `/api/v1/analyses/*`

## Local Run

```powershell
.\scripts\setup.ps1
.\venv\Scripts\python app/main.py
```

Python 3.14 is required. The setup script replaces an incompatible local `venv`, installs
dependencies using Python 3.14 wheels, and creates `.env` from `.env.example` when needed.

Set `KAFKA_CONSUMER_ENABLED=true` only when Kafka is running.
Set `OPENCODE_API_KEY` in `.env`; models are attempted in `OPENCODE_MODELS` order.
Configure the bind address with `HOST` and `PORT`.
