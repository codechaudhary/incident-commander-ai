from __future__ import annotations
import asyncio
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

    async def process_from_trigger(
        self,
        request: TriggerAnalysisRequest,
        analysis_id: str,
    ) -> None:
        """
        Runs LLM analysis for a trace that was triggered via the REST API
        (not via Kafka). Builds a TraceEventPayload from the request context.
        """
        from datetime import timezone, datetime
        from app.models.enums import FailureType
        from app.models.schemas import ErrorSpan

        try:
            async with self.repository_factory() as repository:
                await repository.mark_processing(analysis_id)

            now = datetime.now(timezone.utc)
            # Use provided data or fallback to defaults if minimal trigger was used
            payload = TraceEventPayload(
                trace_id=request.traceId,
                root_service=request.rootService or "unknown",
                root_operation=request.rootOperation or "unknown",
                status=request.status or "ERROR",
                failure_type=FailureType(request.failureType) if request.failureType else FailureType.NONE,
                duration_ms=request.durationMs or 1,
                started_at=now,
                ended_at=now,
                span_count=1 + (len(request.errorSpans) if request.errorSpans else 0),
                error_spans=[
                    ErrorSpan(
                        span_id=span.get("spanId", "unknown"),
                        service_name=span.get("serviceName", "unknown"),
                        operation=span.get("operation", "unknown"),
                        error_message=span.get("errorMessage", None),
                        duration_ms=span.get("durationMs", 0),
                    )
                    for span in (request.errorSpans or [])
                ],
            )

            llm_response = await self.llm_service.analyze(payload)
            async with self.repository_factory() as repository:
                completed = await repository.mark_completed(
                    analysis_id,
                    llm_response.result,
                    model_used=llm_response.model_used,
                    prompt_tokens=llm_response.prompt_tokens,
                    completion_tokens=llm_response.completion_tokens,
                    latency_ms=llm_response.latency_ms,
                )
                dto = to_analysis_dto(completed)
            await self.publisher.publish_completed(dto)
            logger.info("analysis_completed", trace_id=request.traceId, analysis_id=analysis_id)
        except Exception as exc:
            async with self.repository_factory() as repository:
                await repository.mark_failed(analysis_id, str(exc))
            logger.exception("analysis_failed", trace_id=request.traceId, error=str(exc))

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
                    latency_ms=llm_response.latency_ms,
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
