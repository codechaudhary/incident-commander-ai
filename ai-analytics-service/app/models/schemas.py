from datetime import datetime
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field

from app.models.enums import AnalysisStatus, FailureType, TraceStatus


def to_camel(value: str) -> str:
    parts = value.split("_")
    return parts[0] + "".join(part.title() for part in parts[1:])


class CamelModel(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True, use_enum_values=True)


class ErrorSpan(CamelModel):
    span_id: str
    service_name: str
    operation: str
    error_message: str | None = None
    duration_ms: int = Field(ge=0)


class TraceEventPayload(CamelModel):
    trace_id: str
    root_service: str
    root_operation: str
    status: TraceStatus
    failure_type: FailureType = FailureType.NONE
    duration_ms: int = Field(gt=0)
    started_at: datetime
    ended_at: datetime
    span_count: int = Field(ge=1)
    error_spans: list[ErrorSpan] = Field(default_factory=list)


class KafkaEnvelope(CamelModel):
    event_id: UUID
    event_type: str
    event_version: str
    timestamp: datetime
    source: str
    payload: TraceEventPayload


class LLMAnalysisResult(CamelModel):
    root_cause: str = Field(min_length=1, max_length=2000)
    affected_services: list[str] = Field(default_factory=list)
    recommendations: list[str] = Field(default_factory=list, min_length=1, max_length=4)
    confidence_score: float = Field(ge=0.0, le=1.0)


class AnalysisDto(CamelModel):
    id: str
    analysis_id: str
    trace_id: str
    alert_id: str | None = None
    status: AnalysisStatus
    root_cause: str | None = None
    affected_services: list[str] = Field(default_factory=list)
    recommendations: list[str] = Field(default_factory=list)
    confidence_score: float | None = None
    model_used: str | None = None
    prompt_tokens: int | None = None
    completion_tokens: int | None = None
    created_at: datetime
    completed_at: datetime | None = None


class PendingAnalysisResponse(CamelModel):
    analysis_id: str
    trace_id: str
    status: AnalysisStatus
    message: str


class TriggerAnalysisRequest(CamelModel):
    trace_id: str
    alert_id: str | None = None


class HealthResponse(BaseModel):
    status: str
