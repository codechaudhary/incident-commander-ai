from __future__ import annotations
from datetime import timezone, datetime
from uuid import uuid4

from sqlalchemy import select, update
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.database import AIAnalysis
from app.models.enums import AnalysisStatus
from app.models.schemas import AnalysisDto, LLMAnalysisResult


def to_analysis_dto(row: AIAnalysis) -> AnalysisDto:
    return AnalysisDto(
        id=str(row.id),
        analysis_id=row.analysis_id,
        trace_id=row.trace_id,
        alert_id=row.alert_id,
        status=AnalysisStatus(row.status),
        root_cause=row.root_cause,
        affected_services=row.affected_services or [],
        recommendations=row.recommendations or [],
        confidence_score=float(row.confidence_score) if row.confidence_score is not None else None,
        model_used=row.model_used,
        prompt_tokens=row.prompt_tokens,
        completion_tokens=row.completion_tokens,
        created_at=row.created_at,
        completed_at=row.completed_at,
    )


class AnalysisRepository:
    def __init__(self, session: AsyncSession) -> None:
        self.session = session

    async def get_by_trace_id(self, trace_id: str) -> AIAnalysis | None:
        result = await self.session.execute(
            select(AIAnalysis)
            .where(AIAnalysis.trace_id == trace_id)
            .order_by(AIAnalysis.created_at.desc())
        )
        return result.scalars().first()

    async def create_pending(self, trace_id: str, alert_id: str | None = None) -> AIAnalysis:
        existing = await self.get_by_trace_id(trace_id)
        active_statuses = {AnalysisStatus.PENDING.value, AnalysisStatus.PROCESSING.value}
        if existing and existing.status in active_statuses:
            return existing

        row = AIAnalysis(
            analysis_id=str(uuid4()),
            trace_id=trace_id,
            alert_id=alert_id,
            status=AnalysisStatus.PENDING.value,
        )
        self.session.add(row)
        await self.session.commit()
        await self.session.refresh(row)
        return row

    async def mark_processing(self, analysis_id: str) -> None:
        await self.session.execute(
            update(AIAnalysis)
            .where(AIAnalysis.analysis_id == analysis_id)
            .values(status=AnalysisStatus.PROCESSING.value)
        )
        await self.session.commit()

    async def mark_completed(
        self,
        analysis_id: str,
        result: LLMAnalysisResult,
        *,
        model_used: str,
        prompt_tokens: int | None,
        completion_tokens: int | None,
    ) -> AIAnalysis:
        await self.session.execute(
            update(AIAnalysis)
            .where(AIAnalysis.analysis_id == analysis_id)
            .values(
                status=AnalysisStatus.COMPLETED.value,
                root_cause=result.root_cause,
                affected_services=result.affected_services,
                recommendations=result.recommendations,
                confidence_score=result.confidence_score,
                model_used=model_used,
                prompt_tokens=prompt_tokens,
                completion_tokens=completion_tokens,
                completed_at=datetime.now(timezone.utc),
            )
        )
        await self.session.commit()
        row = await self.get_by_analysis_id(analysis_id)
        if row is None:
            raise RuntimeError(f"Analysis {analysis_id} disappeared after update")
        return row

    async def mark_failed(self, analysis_id: str, message: str) -> None:
        await self.session.execute(
            update(AIAnalysis)
            .where(AIAnalysis.analysis_id == analysis_id)
            .values(
                status=AnalysisStatus.FAILED.value,
                root_cause=message[:2000],
                completed_at=datetime.now(timezone.utc),
            )
        )
        await self.session.commit()

    async def get_by_analysis_id(self, analysis_id: str) -> AIAnalysis | None:
        result = await self.session.execute(
            select(AIAnalysis).where(AIAnalysis.analysis_id == analysis_id)
        )
        return result.scalars().first()
