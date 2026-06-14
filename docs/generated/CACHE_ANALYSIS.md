# CACHE AND REDIS ANALYSIS

This document details how Redis is utilized across the architecture for caching, persistence, and event relaying.

## 1. Global Redis Configuration
* **Instance:** A single `redis:7-alpine` container defined in `docker-compose.yml`.
* **Port:** 6379
* **Memory Policy:** Configured with `redis-server --maxmemory 128mb --maxmemory-policy allkeys-lru`. This dictates that if Redis exceeds 128MB, it will evict the least recently used keys, regardless of TTL. This is dangerous if Redis is used as a primary datastore.

## 2. AI Analytics Service Usage
The `ai-analytics-service` uses Redis as its primary persistence and state management store for analysis results, contrary to standard caching patterns.

* **Client:** `redis.asyncio` (`ai-analytics-service/app/db/redis_repository.py`).
* **Key Patterns:**
  * `ai_analysis:{analysis_id}`: Stores the full JSON payload of the LLM analysis (`RedisAnalysisRecord`).
  * `trace_to_analysis:{trace_id}`: Acts as a secondary index mapping a trace ID to the generated analysis ID.
* **TTL Strategy:** **NOT FOUND IN CODEBASE.** Keys are set using `await self.client.set(key, data)` without an `ex` (expire) parameter. Because the global Redis policy is `allkeys-lru`, these records will persist indefinitely until the 128MB memory limit is hit, at which point active analyses might be randomly evicted.
* **Read Path:** `get_by_trace_id` reads the `trace_to_analysis` index, then fetches the actual payload from `ai_analysis`.

## 3. BFF Service Usage (Pub/Sub)
The `bff-service` uses Redis exclusively as a Pub/Sub message broker to power WebSockets, not as a key-value cache.

* **Client:** Spring `ReactiveRedisConnectionFactory` with `ReactiveRedisMessageListenerContainer`.
* **Channels Subscribed To:**
  * `alerts:live`
  * `analysis:live`
  * `traces:live`
* **Behavior:** When a message is published to these Redis channels, the BFF service's `RedisMessageRelay.java` routes the payload directly to the frontend via STOMP WebSockets.
* **Durability:** Redis Pub/Sub is inherently "fire-and-forget". If the BFF service restarts or is down while a message is published to the Redis channel, the message is lost and will not reach the frontend WebSocket clients.

## 4. Missing Cache Implementations
> [!WARNING]
> **No API Caching:** The BFF `DashboardService.java` makes 6 concurrent HTTP calls to downstream services on every page load. There is no `@Cacheable` annotation or programmatic cache to store these aggregate results. Every dashboard refresh generates load on the backend services and databases.
