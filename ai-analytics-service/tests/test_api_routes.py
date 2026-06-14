import warnings

import pytest
from fastapi import FastAPI
from httpx import ASGITransport, AsyncClient
from pydantic.warnings import UnsupportedFieldAttributeWarning

import app.main_state as state
from app.api.routes.analysis import router
from app.core.config import get_settings
from app.db.json_repository import JsonAnalysisStore
from app.redis.publisher import AnalysisPublisher
from app.services.analysis_service import AnalysisService
from app.services.llm_service import LLMService


@pytest.mark.filterwarnings("error::pydantic.warnings.UnsupportedFieldAttributeWarning")
async def test_trigger_and_get_analysis_do_not_emit_alias_warnings(tmp_path):
    settings = get_settings()
    store = JsonAnalysisStore(str(tmp_path / "ai_analysis.json"))
    state.analysis_service = AnalysisService(
        repository_factory=store.repository,
        llm_service=LLMService(settings),
        publisher=AnalysisPublisher(settings),
    )

    app = FastAPI()
    app.include_router(router, prefix=settings.api_prefix)

    with warnings.catch_warnings():
        warnings.simplefilter("error", UnsupportedFieldAttributeWarning)
        app.openapi()
        async with AsyncClient(
            transport=ASGITransport(app=app),
            base_url="http://test",
        ) as client:
            trigger_response = await client.post(
                "/api/v1/analyses/trigger",
                json={"traceId": "trace-123", "alertId": "alert-123"},
            )
            get_response = await client.get("/api/v1/analyses/trace-123")

    await state.analysis_service.llm_service.close()
    await state.analysis_service.publisher.close()
    state.analysis_service = None

    assert trigger_response.status_code == 202
    assert get_response.status_code == 202
    assert trigger_response.json()["traceId"] == "trace-123"
    assert get_response.json()["traceId"] == "trace-123"
