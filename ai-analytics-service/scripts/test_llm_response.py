# ruff: noqa: E402,I001
import argparse
import asyncio
import json
import sys
from datetime import UTC, datetime
from pathlib import Path


ROOT_DIR = Path(__file__).resolve().parents[1]
if str(ROOT_DIR) not in sys.path:
    sys.path.insert(0, str(ROOT_DIR))

from app.core.config import Settings
from app.models.schemas import ErrorSpan, TraceEventPayload
from app.services.llm_service import LLMService


def build_payload() -> TraceEventPayload:
    now = datetime.now(UTC)
    return TraceEventPayload(
        traceId="manual-llm-test-trace",
        rootService="checkout-service",
        rootOperation="POST /checkout",
        status="ERROR",
        failureType="DB_TIMEOUT",
        durationMs=4850,
        startedAt=now,
        endedAt=now,
        spanCount=3,
        errorSpans=[
            ErrorSpan(
                spanId="span-payment-db",
                serviceName="payment-service",
                operation="SELECT payment_method",
                errorMessage="database query timed out after 3000ms",
                durationMs=3012,
            )
        ],
    )


def settings_for_run(args: argparse.Namespace) -> Settings:
    updates: dict[str, object] = {}
    if args.provider:
        updates["llm_provider"] = args.provider
    if args.models:
        updates["opencode_models"] = args.models
    if args.timeout:
        updates["llm_timeout_seconds"] = args.timeout
    return Settings(**updates)


async def run(args: argparse.Namespace) -> int:
    settings = settings_for_run(args)
    print(f"Provider: {settings.llm_provider}")
    print(f"Models: {', '.join(settings.llm_models)}")

    if settings.llm_provider == "opencode" and not settings.opencode_api_key:
        print("OPENCODE_API_KEY is required when LLM_PROVIDER=opencode.", file=sys.stderr)
        return 2

    service = LLMService(settings)
    try:
        response = await service.analyze(build_payload())
    finally:
        await service.close()

    print("\nLLM responded successfully.")
    print(f"Model used: {response.model_used}")
    print(f"Prompt tokens: {response.prompt_tokens}")
    print(f"Completion tokens: {response.completion_tokens}")
    print("\nParsed contract response:")
    print(json.dumps(response.result.model_dump(by_alias=True), indent=2))
    return 0


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Send a sample trace through the ai-analytics-service LLMService."
    )
    parser.add_argument(
        "--provider",
        choices=("opencode", "stub"),
        help="Override LLM_PROVIDER from .env for this test run.",
    )
    parser.add_argument(
        "--models",
        help="Comma-separated model order to override OPENCODE_MODELS for this test run.",
    )
    parser.add_argument(
        "--timeout",
        type=float,
        help="Override LLM_TIMEOUT_SECONDS for this test run.",
    )
    return parser.parse_args()


if __name__ == "__main__":
    raise SystemExit(asyncio.run(run(parse_args())))
