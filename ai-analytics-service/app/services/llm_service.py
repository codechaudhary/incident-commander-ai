from __future__ import annotations
import json
import time
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
    latency_ms: int | None = None


class LLMService:
    """All analysis goes through the real LLM. No stubs, no hardcoded responses."""

    def __init__(self, settings: Settings, client: httpx.AsyncClient | None = None) -> None:
        self.settings = settings
        self._owns_client = client is None
        self._client = client or httpx.AsyncClient(
            base_url=settings.opencode_base_url.rstrip("/") + "/",
            timeout=httpx.Timeout(connect=5.0, read=settings.llm_timeout_seconds, write=5.0, pool=3.0),
            limits=httpx.Limits(max_connections=10, max_keepalive_connections=5),
        )

    async def analyze(self, payload: TraceEventPayload) -> LLMResponse:
        """Send trace data to LLM and return analysis. Never returns stubs."""
        if not self.settings.opencode_api_key:
            raise RuntimeError("OPENCODE_API_KEY is required — no stub/hardcoded fallback exists")

        user_prompt = build_user_prompt(payload)
        request_json = {
            "model": None,  # filled per-model below
            "temperature": self.settings.llm_temperature,
            "max_tokens": self.settings.llm_max_tokens,
            "response_format": {"type": "json_object"},
            "messages": [
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": user_prompt},
            ],
        }

        last_error: Exception | None = None
        for model in self.settings.llm_models:
            try:
                request_json["model"] = model
                t0 = time.monotonic()
                response = await self._client.post(
                    "chat/completions",
                    headers={"Authorization": f"Bearer {self.settings.opencode_api_key}"},
                    json=request_json,
                )
                response.raise_for_status()
                latency_ms = int((time.monotonic() - t0) * 1000)

                body = response.json()
                raw_content = body["choices"][0]["message"]["content"]
                result = self._parse_result(raw_content)
                usage = body.get("usage") or {}

                logger.info(
                    "llm_call_success",
                    model=model,
                    latency_ms=latency_ms,
                    prompt_tokens=usage.get("prompt_tokens"),
                    completion_tokens=usage.get("completion_tokens"),
                )

                return LLMResponse(
                    result=result,
                    model_used=model,
                    prompt_tokens=usage.get("prompt_tokens"),
                    completion_tokens=usage.get("completion_tokens"),
                    latency_ms=latency_ms,
                )
            except Exception as exc:
                last_error = exc
                logger.warning(
                    "llm_model_failed",
                    model=model,
                    error_type=type(exc).__name__,
                    error=str(exc),
                )

        raise RuntimeError("All configured LLM models failed") from last_error

    async def close(self) -> None:
        if self._owns_client:
            await self._client.aclose()

    def _parse_result(self, content: str) -> LLMAnalysisResult:
        try:
            return LLMAnalysisResult.model_validate(json.loads(content))
        except (json.JSONDecodeError, ValidationError) as exc:
            logger.warning("llm_response_parse_failed", error=str(exc), content=content[:500])
            raise ValueError("LLM response did not match the expected JSON contract") from exc
