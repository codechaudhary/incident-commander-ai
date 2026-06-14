from collections.abc import Callable
from contextlib import AbstractAsyncContextManager

import structlog

from app.db.repository import to_analysis_dto
from app.models.enums import TraceStatus
from app.models.schemas import AnalysisDto, PendingAnalysisResponse, TraceEventPayload
from app.redis.publisher import AnalysisPublisher
from app.services.llm_service import LLMService

logger = structlog.get_logger(__name__)


class AnalysisService:
    def __init__(
        self,
        *,
        repository_factory: Callable[[], AbstractAsyncContextManager],
        llm_service: LLMService,
        publisher: AnalysisPublisher,
    ) -> None:
        self.repository_factory = repository_factory
        self.llm_service = llm_service
        self.publisher = publisher

    async def get_by_trace_id(self, trace_id: str) -> AnalysisDto | PendingAnalysisResponse | None:
        async with self.repository_factory() as repository:
            row = await repository.get_by_trace_id(trace_id)
            if row is None:
                return None
            if row.status in {"PENDING", "PROCESSING"}:
                return PendingAnalysisResponse(
                    analysis_id=row.analysis_id,
                    trace_id=row.trace_id,
                    status=row.status,
                    message="Analysis in progress",
                )
            return to_analysis_dto(row)

    async def trigger(self, trace_id: str, alert_id: str | None = None) -> PendingAnalysisResponse:
        async with self.repository_factory() as repository:
            row = await repository.create_pending(trace_id, alert_id)
            return PendingAnalysisResponse(
                analysis_id=row.analysis_id,
                trace_id=row.trace_id,
                status=row.status,
                message="Analysis queued",
            )

    async def process_trace_event(
        self,
        payload: TraceEventPayload,
        alert_id: str | None = None,
    ) -> None:
        if payload.status == TraceStatus.SUCCESS:
            logger.info("trace_skipped_success", trace_id=payload.trace_id)
            return

        async with self.repository_factory() as repository:
            row = await repository.create_pending(payload.trace_id, alert_id)
            await repository.mark_processing(row.analysis_id)

        try:
            llm_response = await self.llm_service.analyze(payload)
            async with self.repository_factory() as repository:
                completed = await repository.mark_completed(
                    row.analysis_id,
                    llm_response.result,
                    model_used=llm_response.model_used,
                    prompt_tokens=llm_response.prompt_tokens,
                    completion_tokens=llm_response.completion_tokens,
                )
                dto = to_analysis_dto(completed)
            await self.publisher.publish_completed(dto)
            logger.info(
                "analysis_completed",
                trace_id=payload.trace_id,
                analysis_id=row.analysis_id,
            )
        except Exception as exc:
            async with self.repository_factory() as repository:
                await repository.mark_failed(row.analysis_id, str(exc))
            logger.exception("analysis_failed", trace_id=payload.trace_id, error=str(exc))
