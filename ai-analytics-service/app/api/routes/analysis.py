from typing import Annotated

from fastapi import APIRouter, Depends, Response, status

from app.core.errors import NotFoundError
from app.main_state import get_analysis_service
from app.models.enums import AnalysisStatus
from app.models.schemas import AnalysisDto, PendingAnalysisResponse, TriggerAnalysisRequest
from app.services.analysis_service import AnalysisService

router = APIRouter(prefix="/analyses", tags=["analyses"])
AnalysisServiceDep = Annotated[AnalysisService, Depends(get_analysis_service)]


@router.get(
    "/{trace_id}",
    response_model=AnalysisDto | PendingAnalysisResponse,
    response_model_by_alias=True,
)
async def get_analysis(
    trace_id: str,
    response: Response,
    service: AnalysisServiceDep,
) -> AnalysisDto | PendingAnalysisResponse:
    analysis = await service.get_by_trace_id(trace_id)
    if analysis is None:
        raise NotFoundError(f"Analysis for trace id '{trace_id}' not found")
    if isinstance(analysis, PendingAnalysisResponse):
        response.status_code = status.HTTP_202_ACCEPTED
    return analysis


@router.post(
    "/trigger",
    response_model=PendingAnalysisResponse,
    response_model_by_alias=True,
    status_code=status.HTTP_202_ACCEPTED,
)
async def trigger_analysis(
    request: TriggerAnalysisRequest,
    service: AnalysisServiceDep,
) -> PendingAnalysisResponse:
    queued = await service.trigger(request.trace_id, request.alert_id)
    queued.status = AnalysisStatus.PENDING
    queued.message = "Analysis queued"
    return queued
