from datetime import datetime
from uuid import uuid4

from sqlalchemy import DateTime, Integer, Numeric, String, Text, func
from sqlalchemy.dialects.postgresql import ARRAY, UUID
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column

from app.models.enums import AnalysisStatus


class Base(DeclarativeBase):
    pass


class AIAnalysis(Base):
    __tablename__ = "ai_analysis"

    id: Mapped[str] = mapped_column(
        UUID(as_uuid=False),
        primary_key=True,
        default=lambda: str(uuid4()),
    )
    analysis_id: Mapped[str] = mapped_column(String(64), unique=True, nullable=False)
    trace_id: Mapped[str] = mapped_column(String(64), nullable=False, index=True)
    alert_id: Mapped[str | None] = mapped_column(String(64), nullable=True)
    status: Mapped[str] = mapped_column(
        String(16), nullable=False, default=AnalysisStatus.PENDING.value, index=True
    )
    root_cause: Mapped[str | None] = mapped_column(Text, nullable=True)
    affected_services: Mapped[list[str] | None] = mapped_column(ARRAY(Text), nullable=True)
    recommendations: Mapped[list[str] | None] = mapped_column(ARRAY(Text), nullable=True)
    confidence_score: Mapped[float | None] = mapped_column(Numeric(4, 3), nullable=True)
    model_used: Mapped[str | None] = mapped_column(String(128), nullable=True)
    prompt_tokens: Mapped[int | None] = mapped_column(Integer, nullable=True)
    completion_tokens: Mapped[int | None] = mapped_column(Integer, nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
    completed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
