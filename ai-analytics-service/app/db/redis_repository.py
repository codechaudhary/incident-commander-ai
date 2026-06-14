from __future__ import annotations
import json
from contextlib import asynccontextmanager
from dataclasses import asdict, dataclass, field
from datetime import timezone, datetime
from uuid import uuid4

import redis.asyncio as redis

from app.models.enums import AnalysisStatus
from app.models.schemas import LLMAnalysisResult


@dataclass
class RedisAnalysisRecord:
    id: str
    analysis_id: str
    trace_id: str
    alert_id: str | None
    status: str
    root_cause: str | None = None
    affected_services: list[str] = field(default_factory=list)
    recommendations: list[str] = field(default_factory=list)
    confidence_score: float | None = None
    model_used: str | None = None
    prompt_tokens: int | None = None
    completion_tokens: int | None = None
    latency_ms: int | None = None
    created_at: datetime = field(default_factory=lambda: datetime.now(timezone.utc))
    completed_at: datetime | None = None

    @classmethod
    def from_dict(cls, value: dict) -> "RedisAnalysisRecord":
        return cls(
            **{
                **value,
                "created_at": datetime.fromisoformat(value["created_at"]),
                "completed_at": (
                    datetime.fromisoformat(value["completed_at"])
                    if value.get("completed_at")
                    else None
                ),
            }
        )

    def to_dict(self) -> dict:
        value = asdict(self)
        value["created_at"] = self.created_at.isoformat()
        value["completed_at"] = self.completed_at.isoformat() if self.completed_at else None
        return value


class RedisAnalysisStore:
    def __init__(self, redis_url: str) -> None:
        self._client = redis.from_url(redis_url, decode_responses=True)

    async def close(self):
        await self._client.aclose()

    @asynccontextmanager
    async def repository(self):
        yield RedisAnalysisRepository(self._client)


class RedisAnalysisRepository:
    def __init__(self, client: redis.Redis) -> None:
        self.client = client
        self.prefix = "ai_analysis:"
        self.trace_index_prefix = "trace_to_analysis:"

    async def get_by_trace_id(self, trace_id: str) -> RedisAnalysisRecord | None:
        analysis_id = await self.client.get(f"{self.trace_index_prefix}{trace_id}")
        if not analysis_id:
            return None
        return await self.get_by_analysis_id(analysis_id)

    async def create_pending(
        self,
        trace_id: str,
        alert_id: str | None = None,
    ) -> RedisAnalysisRecord:
        existing = await self.get_by_trace_id(trace_id)
        active_statuses = {AnalysisStatus.PENDING.value, AnalysisStatus.PROCESSING.value}

        if existing and existing.status in active_statuses:
            age_seconds = (datetime.now(timezone.utc) - existing.created_at.replace(tzinfo=timezone.utc)).total_seconds()
            if age_seconds < 180:
                return existing
            existing.status = AnalysisStatus.FAILED.value
            existing.root_cause = "Timed out waiting for processing"
            await self._save(existing)

        row = RedisAnalysisRecord(
            id=str(uuid4()),
            analysis_id=str(uuid4()),
            trace_id=trace_id,
            alert_id=alert_id,
            status=AnalysisStatus.PENDING.value,
        )
        await self._save(row)
        return row

    async def mark_processing(self, analysis_id: str) -> None:
        row = await self.get_by_analysis_id(analysis_id)
        if row:
            row.status = AnalysisStatus.PROCESSING.value
            await self._save(row)

    async def mark_completed(
        self,
        analysis_id: str,
        result: LLMAnalysisResult,
        *,
        model_used: str,
        prompt_tokens: int | None,
        completion_tokens: int | None,
        latency_ms: int | None = None,
    ) -> RedisAnalysisRecord:
        row = await self.get_by_analysis_id(analysis_id)
        if row is None:
            raise RuntimeError(f"Analysis {analysis_id} disappeared after update")

        row.status = AnalysisStatus.COMPLETED.value
        row.root_cause = result.root_cause
        row.affected_services = result.affected_services
        row.recommendations = result.recommendations
        row.confidence_score = result.confidence_score
        row.model_used = model_used
        row.prompt_tokens = prompt_tokens
        row.completion_tokens = completion_tokens
        row.latency_ms = latency_ms
        row.completed_at = datetime.now(timezone.utc)
        await self._save(row)
        return row

    async def mark_failed(self, analysis_id: str, message: str) -> None:
        row = await self.get_by_analysis_id(analysis_id)
        if row:
            row.status = AnalysisStatus.FAILED.value
            row.root_cause = message[:2000]
            row.completed_at = datetime.now(timezone.utc)
            await self._save(row)

    async def get_by_analysis_id(self, analysis_id: str) -> RedisAnalysisRecord | None:
        data = await self.client.get(f"{self.prefix}{analysis_id}")
        if data:
            return RedisAnalysisRecord.from_dict(json.loads(data))
        return None

    async def _save(self, row: RedisAnalysisRecord) -> None:
        data = json.dumps(row.to_dict())
        # Save main record
        await self.client.set(f"{self.prefix}{row.analysis_id}", data)
        # Update trace_id -> analysis_id index
        await self.client.set(f"{self.trace_index_prefix}{row.trace_id}", row.analysis_id)
