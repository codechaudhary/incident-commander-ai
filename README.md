# 🚨 Incident Commander AI

> **AI-powered production incident management platform** — ingests distributed traces, identifies root causes using LLMs, and alerts engineers in real-time. Built with Java Spring Boot, Python FastAPI, Kafka, and React.

[![Java](https://img.shields.io/badge/Java-17-blue?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Python](https://img.shields.io/badge/Python-3.11-3776AB?style=flat-square&logo=python)](https://python.org)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.110-009688?style=flat-square&logo=fastapi)](https://fastapi.tiangolo.com/)
[![Kafka](https://img.shields.io/badge/Apache_Kafka-3.7-231F20?style=flat-square&logo=apachekafka)](https://kafka.apache.org/)
[![React](https://img.shields.io/badge/React-18-61DAFB?style=flat-square&logo=react)](https://react.dev/)

---

## 📋 Table of Contents

- [What it does](#-what-it-does)
- [Architecture](#-architecture)
- [Tech stack](#-tech-stack)
- [Microservices](#-microservices)
- [Getting started](#-getting-started)
- [Authors](#-authors)

---

## 🔍 What it does

Large engineering teams lose hours during production incidents manually searching logs, metrics, dashboards, and past tickets. **Incident Commander AI** acts as an automated first responder:

1. **Ingests** distributed microservice traces and span trees via REST
2. **Identifies root cause** automatically using an LLM analysis pipeline powered by Anthropic's Claude
3. **Alerts** on-call engineers via real-time WebSocket push with severity-ranked alerts
4. **Displays** everything on a live React dashboard — DAG service graph, span waterfall, AI narration panel, and alert center

### User flow

```text
Engineer POSTs a trace  →  Kafka triggers AI analysis  →  Root cause identified
       ↓                                                          ↓
  DAG graph turns red   ←   WebSocket push to dashboard  ←  Alert fires
       ↓
  Click trace → see span waterfall + AI narration
```

---

## 🏗️ Architecture

### High-level

```text
┌─────────────────────────────────────────────────────┐
│                   React SPA                         │
│    Dashboard · Traces · Alerts · DAG Graph          │
└─────────────────────┬───────────────────────────────┘
                      │  REST + WebSocket
┌─────────────────────▼───────────────────────────────┐
│           BFF / API Gateway                         │
│         Spring WebFlux · Redis WebSocket Relay      │
└──┬──────────────┬────────────────┬──────────────────┘
   │              │                │
   ▼              ▼                ▼
[Java Services]  [Kafka Bus]   [Python Services]
Trace Service  ──────────►    AI Analytics Service
Alert Service                 (Anthropic Claude API)
Simulator
   │                               │
   ▼                               ▼
[PostgreSQL ×2]              [Redis Cache]
```

---

## 🛠️ Tech stack

| Layer | Technology |
|---|---|
| **Java backend** | Spring Boot 3.5, Spring WebFlux |
| **Python backend** | FastAPI, asyncio, httpx |
| **AI / LLM** | Anthropic Claude API |
| **Event streaming** | Apache Kafka |
| **Cache / pub-sub** | Redis |
| **Primary databases** | PostgreSQL |
| **Frontend** | React 18, TailwindCSS, Recharts, React Flow |
| **State management** | Zustand + TanStack Query |

---

## 📦 Microservices

#### `bff-service` (API Gateway)
Uses **Spring WebFlux** for non-blocking parallel requests to downstream services. Acts as a WebSocket server that subscribes to Redis pub/sub channels (`alerts:live`, `analysis:live`) to fan out real-time events to connected UI clients.

#### `trace-service` (Ingestion)
Accepts distributed trace payloads, walks span trees to compute durations and detect error statuses, and persists them to PostgreSQL. Publishes `trace.ingested` events to Kafka.

#### `alert-service` (Rules-based alerting)
Listens to Kafka events, evaluates them for failures, fires alerts based on severity, and exposes a REST API for alert state management.

#### `ai-analytics-service` (LLM Engine)
FastAPI service that listens to `trace.ingested` Kafka events and asynchronously triggers LLM-powered root cause analysis using Anthropic Claude. Caches pending and completed states in Redis.

#### `order-simulator` (Load Generation)
Provides a synthetic testing endpoint to generate complex realistic trace span trees simulating various failure types (e.g., DB timeouts, cascading failures) for testing.

---

## 🚀 Getting started

### Prerequisites

- Docker and Docker Compose
- An Anthropic API key (for Claude)

### 1. Clone the repository

```bash
git clone https://github.com/codechaudhary/incident-commander-ai.git
cd incident-commander-ai
```

### 2. Configure environment

Create a `.env` file in the root directory (you can use `.env.example` as a template):

```bash
cp .env.example .env
```

Edit `.env` and fill in your `ANTHROPIC_API_KEY`.

### 3. Start all services

```bash
docker-compose up --build
```

This starts:
- PostgreSQL instances
- Redis
- Apache Kafka + Zookeeper
- All microservices
- React frontend

Wait for all services to report healthy (~60 seconds on first run).

### 4. Seed demo data

To test the platform, you can run the simulator to generate synthetic failure traces:

```bash
curl -X POST http://localhost:8085/api/v1/simulate \
  -H "Content-Type: application/json" \
  -d '{"failureType": "DB_CONNECTION_ERROR"}'
```

### 5. Open the dashboard

Navigate to **http://localhost:3000** to view the live traces, alerts, and AI narrations streaming in real-time.

---

## 👥 Authors

| Name | Role | GitHub |
|---|---|---|
| Harshit Chaudhary | Core Developer | [@codeChaudhary](https://github.com/codechaudhary) |