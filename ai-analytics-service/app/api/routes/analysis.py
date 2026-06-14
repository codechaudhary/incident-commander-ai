from typing import Annotated

from fastapi import APIRouter, Depends, status
from fastapi.responses import JSONResponse

from app.core.errors import NotFoundError
from app.main_state import get_analysis_service
from app.models.enums import AnalysisStatus
from app.models.schemas import PendingAnalysisResponse, TriggerAnalysisRequest
from app.services.analysis_service import AnalysisService

router = APIRouter(prefix="/analyses", tags=["analyses"])
AnalysisServiceDep = Annotated[AnalysisService, Depends(get_analysis_service)]


@router.get(
    "/{trace_id}",
    response_model=None,
)
async def get_analysis(
    trace_id: str,
    service: AnalysisServiceDep,
) -> JSONResponse:
    analysis = await service.get_by_trace_id(trace_id)
    if analysis is None:
        raise NotFoundError(f"Analysis for trace id '{trace_id}' not found")
    status_code = (
        status.HTTP_202_ACCEPTED
        if isinstance(analysis, PendingAnalysisResponse)
        else status.HTTP_200_OK
    )
    return JSONResponse(
        content=analysis.model_dump(mode="json", by_alias=True),
        status_code=status_code,
    )


@router.post(
    "/trigger",
    response_model=None,
    status_code=status.HTTP_202_ACCEPTED,
)
async def trigger_analysis(
    body: TriggerAnalysisRequest,
    service: AnalysisServiceDep,
) -> JSONResponse:
    queued = await service.trigger(body.traceId, body.alertId)
    queued.status = AnalysisStatus.PENDING
    queued.message = "Analysis queued"
    return JSONResponse(
        content=queued.model_dump(mode="json", by_alias=True),
        status_code=status.HTTP_202_ACCEPTED,
    )
