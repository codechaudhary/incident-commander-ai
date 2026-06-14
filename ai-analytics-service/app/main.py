# ruff: noqa: E402
import sys
from contextlib import asynccontextmanager
from pathlib import Path

if __package__ in {None, ""}:
    app_dir = Path(__file__).resolve().parent
    project_root = app_dir.parent
    sys.path = [path for path in sys.path if Path(path or ".").resolve() != app_dir]
    sys.path.insert(0, str(project_root))

import structlog
import uvicorn
from fastapi import FastAPI
from sqlalchemy import text

from app.api.routes.analysis import router as analysis_router
from app.core.config import get_settings
from app.core.errors import NotFoundError, not_found_handler, unhandled_exception_handler
from app.core.logging import configure_logging
from app.db.session import AsyncSessionLocal, engine
from app.kafka.consumer import TraceEventConsumer
from app.models.schemas import HealthResponse
from app.redis.publisher import AnalysisPublisher
from app.services.analysis_service import AnalysisService
from app.services.llm_service import LLMService

settings = get_settings()
configure_logging(settings.log_level)
logger = structlog.get_logger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    import app.main_state as state

    publisher = AnalysisPublisher(settings)
    llm_service = LLMService(settings)
    analysis_service = AnalysisService(
        session_factory=AsyncSessionLocal,
        llm_service=llm_service,
        publisher=publisher,
    )
    state.analysis_service = analysis_service

    consumer: TraceEventConsumer | None = None
    if settings.kafka_consumer_enabled:
        consumer = TraceEventConsumer(settings, analysis_service)
        await consumer.start()

    logger.info("service_started", app_name=settings.app_name, environment=settings.environment)
    try:
        yield
    finally:
        if consumer:
            await consumer.stop()
        await llm_service.close()
        await publisher.close()
        await engine.dispose()
        state.analysis_service = None
        logger.info("service_stopped")


app = FastAPI(
    title="Incident Commander AI Analysis Service",
    version="0.1.0",
    lifespan=lifespan,
)

app.add_exception_handler(NotFoundError, not_found_handler)
app.add_exception_handler(Exception, unhandled_exception_handler)
app.include_router(analysis_router, prefix=settings.api_prefix)


@app.get("/health", response_model=HealthResponse)
async def health() -> HealthResponse:
    return HealthResponse(status="ok")


@app.get("/ready", response_model=HealthResponse)
async def ready() -> HealthResponse:
    async with engine.connect() as connection:
        await connection.execute(text("SELECT 1"))
    return HealthResponse(status="ok")


if __name__ == "__main__":
    uvicorn.run(app, host=settings.host, port=8080)
