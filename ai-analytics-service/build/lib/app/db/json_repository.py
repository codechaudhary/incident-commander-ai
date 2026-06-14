import asyncio
import json
from contextlib import asynccontextmanager
from dataclasses import asdict, dataclass, field
from datetime import UTC, datetime
from pathlib import Path
from uuid import uuid4

from app.models.enums import AnalysisStatus
from app.models.schemas import LLMAnalysisResult


@dataclass
class JsonAnalysisRecord:
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
    created_at: datetime = field(default_factory=lambda: datetime.now(UTC))
    completed_at: datetime | None = None

    @classmethod
    def from_dict(cls, value: dict) -> "JsonAnalysisRecord":
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


class JsonAnalysisStore:
    def __init__(self, path: str) -> None:
        self.path = Path(path)
        self.lock = asyncio.Lock()

    @asynccontextmanager
    async def repository(self):
        yield JsonAnalysisRepository(self)


class JsonAnalysisRepository:
    def __init__(self, store: JsonAnalysisStore) -> None:
        self.store = store

    async def get_by_trace_id(self, trace_id: str) -> JsonAnalysisRecord | None:
        async with self.store.lock:
            rows = self._load()
            matches = [row for row in rows if row.trace_id == trace_id]
            return max(matches, key=lambda row: row.created_at, default=None)

    async def create_pending(
        self,
        trace_id: str,
        alert_id: str | None = None,
    ) -> JsonAnalysisRecord:
        async with self.store.lock:
            rows = self._load()
            matches = [row for row in rows if row.trace_id == trace_id]
            existing = max(matches, key=lambda row: row.created_at, default=None)
            active_statuses = {AnalysisStatus.PENDING.value, AnalysisStatus.PROCESSING.value}
            if existing and existing.status in active_statuses:
                return existing

            row = JsonAnalysisRecord(
                id=str(uuid4()),
                analysis_id=str(uuid4()),
                trace_id=trace_id,
                alert_id=alert_id,
                status=AnalysisStatus.PENDING.value,
            )
            rows.append(row)
            self._save(rows)
            return row

    async def mark_processing(self, analysis_id: str) -> None:
        async with self.store.lock:
            rows = self._load()
            row = self._find_by_analysis_id(rows, analysis_id)
            if row:
                row.status = AnalysisStatus.PROCESSING.value
                self._save(rows)

    async def mark_completed(
        self,
        analysis_id: str,
        result: LLMAnalysisResult,
        *,
        model_used: str,
        prompt_tokens: int | None,
        completion_tokens: int | None,
    ) -> JsonAnalysisRecord:
        async with self.store.lock:
            rows = self._load()
            row = self._find_by_analysis_id(rows, analysis_id)
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
            row.completed_at = datetime.now(UTC)
            self._save(rows)
            return row

    async def mark_failed(self, analysis_id: str, message: str) -> None:
        async with self.store.lock:
            rows = self._load()
            row = self._find_by_analysis_id(rows, analysis_id)
            if row:
                row.status = AnalysisStatus.FAILED.value
                row.root_cause = message[:2000]
                row.completed_at = datetime.now(UTC)
                self._save(rows)

    async def get_by_analysis_id(self, analysis_id: str) -> JsonAnalysisRecord | None:
        async with self.store.lock:
            return self._find_by_analysis_id(self._load(), analysis_id)

    def _load(self) -> list[JsonAnalysisRecord]:
        if not self.store.path.exists():
            return []
        with self.store.path.open(encoding="utf-8") as file:
            return [JsonAnalysisRecord.from_dict(item) for item in json.load(file)]

    def _save(self, rows: list[JsonAnalysisRecord]) -> None:
        self.store.path.parent.mkdir(parents=True, exist_ok=True)
        with self.store.path.open("w", encoding="utf-8") as file:
            json.dump([row.to_dict() for row in rows], file, indent=2)
            file.write("\n")

    def _find_by_analysis_id(
        self,
        rows: list[JsonAnalysisRecord],
        analysis_id: str,
    ) -> JsonAnalysisRecord | None:
        return next((row for row in rows if row.analysis_id == analysis_id), None)
