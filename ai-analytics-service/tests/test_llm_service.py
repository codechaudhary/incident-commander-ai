import json
from datetime import UTC, datetime

import httpx
import pytest

from app.core.config import Settings
from app.models.schemas import TraceEventPayload
from app.services.llm_service import LLMService


def trace_payload() -> TraceEventPayload:
    now = datetime.now(UTC)
    return TraceEventPayload(
        traceId="trace-1",
        rootService="orders",
        rootOperation="POST /orders",
        status="ERROR",
        failureType="RUNTIME_EXCEPTION",
        durationMs=1000,
        startedAt=now,
        endedAt=now,
        spanCount=1,
    )


@pytest.mark.asyncio
async def test_falls_back_to_next_opencode_model() -> None:
    attempted_models: list[str] = []

    def handler(request: httpx.Request) -> httpx.Response:
        body = json.loads(request.content)
        attempted_models.append(body["model"])
        if body["model"] == "model-1":
            return httpx.Response(503, request=request)
        return httpx.Response(
            200,
            request=request,
            json={
                "choices": [
                    {
                        "message": {
                            "content": json.dumps(
                                {
                                    "rootCause": "Database timeout.",
                                    "affectedServices": ["orders"],
                                    "recommendations": ["Check database capacity."],
                                    "confidenceScore": 0.9,
                                }
                            )
                        }
                    }
                ],
                "usage": {"prompt_tokens": 10, "completion_tokens": 20},
            },
        )

    settings = Settings(
        llm_provider="opencode",
        opencode_api_key="test-key",
        opencode_models="model-1,model-2",
    )
    client = httpx.AsyncClient(
        base_url="https://opencode.test/v1/",
        transport=httpx.MockTransport(handler),
    )
    service = LLMService(settings, client)

    response = await service.analyze(trace_payload())

    assert attempted_models == ["model-1", "model-2"]
    assert response.model_used == "model-2"
    assert response.prompt_tokens == 10
    assert response.result.root_cause == "Database timeout."


@pytest.mark.asyncio
async def test_raises_after_all_opencode_models_fail() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(503, request=request)

    settings = Settings(
        llm_provider="opencode",
        opencode_api_key="test-key",
        opencode_models="model-1,model-2",
    )
    client = httpx.AsyncClient(
        base_url="https://opencode.test/v1/",
        transport=httpx.MockTransport(handler),
    )
    service = LLMService(settings, client)

    with pytest.raises(RuntimeError, match="All configured OpenCode models failed"):
        await service.analyze(trace_payload())
