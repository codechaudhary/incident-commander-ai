import json
from dataclasses import dataclass

import httpx
import structlog
from pydantic import ValidationError

from app.core.config import Settings
from app.models.schemas import LLMAnalysisResult, TraceEventPayload
from app.prompts.root_cause_prompt import SYSTEM_PROMPT, build_user_prompt

logger = structlog.get_logger(__name__)


@dataclass(frozen=True)
class LLMResponse:
    result: LLMAnalysisResult
    model_used: str
    prompt_tokens: int | None = None
    completion_tokens: int | None = None


class LLMService:
    def __init__(self, settings: Settings, client: httpx.AsyncClient | None = None) -> None:
        self.settings = settings
        self._owns_client = client is None
        self._client = client or httpx.AsyncClient(
            base_url=settings.opencode_base_url.rstrip("/") + "/",
            timeout=settings.llm_timeout_seconds,
        )

    async def analyze(self, payload: TraceEventPayload) -> LLMResponse:
        if self.settings.llm_provider == "stub":
            return self._stub_analysis(payload)
        if not self.settings.opencode_api_key:
            raise RuntimeError("OPENCODE_API_KEY is required when LLM_PROVIDER=opencode")

        last_error: Exception | None = None
        for model in self.settings.llm_models:
            try:
                return await self._analyze_model(payload, model)
            except Exception as exc:
                last_error = exc
                logger.warning(
                    "llm_model_failed",
                    model=model,
                    error_type=type(exc).__name__,
                    error=str(exc),
                )

        raise RuntimeError("All configured OpenCode models failed") from last_error

    async def close(self) -> None:
        if self._owns_client:
            await self._client.aclose()

    async def _analyze_model(self, payload: TraceEventPayload, model: str) -> LLMResponse:
        response = await self._client.post(
            "chat/completions",
            headers={"Authorization": f"Bearer {self.settings.opencode_api_key}"},
            json={
                "model": model,
                "temperature": self.settings.llm_temperature,
                "max_tokens": self.settings.llm_max_tokens,
                "response_format": {"type": "json_object"},
                "messages": [
                    {"role": "system", "content": SYSTEM_PROMPT},
                    {"role": "user", "content": build_user_prompt(payload)},
                ],
            },
        )
        response.raise_for_status()
        body = response.json()
        result = self._parse_result(body["choices"][0]["message"]["content"])
        usage = body.get("usage") or {}
        return LLMResponse(
            result=result,
            model_used=model,
            prompt_tokens=usage.get("prompt_tokens"),
            completion_tokens=usage.get("completion_tokens"),
        )

    def _stub_analysis(self, payload: TraceEventPayload) -> LLMResponse:
        services = sorted(
            {payload.root_service, *(span.service_name for span in payload.error_spans)}
        )
        first_error = payload.error_spans[0] if payload.error_spans else None
        failing_service = first_error.service_name if first_error else payload.root_service
        root_cause = (
            f"{failing_service} appears to be the first failing service for trace "
            f"{payload.trace_id}, with status {payload.status} and failure type "
            f"{payload.failure_type}."
        )
        result = LLMAnalysisResult(
            root_cause=root_cause,
            affected_services=services,
            recommendations=[
                f"Inspect recent errors and latency for {failing_service}.",
                "Verify downstream dependency health and connection pool saturation.",
                "Add or tune circuit breakers around the failing dependency path.",
            ],
            confidence_score=0.62,
        )
        return LLMResponse(result=result, model_used="stub-heuristic")

    def _parse_result(self, content: str) -> LLMAnalysisResult:
        try:
            return LLMAnalysisResult.model_validate(json.loads(content))
        except (json.JSONDecodeError, ValidationError) as exc:
            logger.warning("llm_response_parse_failed", error=str(exc), content=content[:500])
            raise ValueError("LLM response did not match the frozen JSON contract") from exc
