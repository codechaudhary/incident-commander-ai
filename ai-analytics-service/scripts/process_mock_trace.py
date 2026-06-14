# ruff: noqa: E402
import argparse
import asyncio
import json
import sys
from datetime import UTC, datetime, timedelta
from pathlib import Path

ROOT_DIR = Path(__file__).resolve().parents[1]
if str(ROOT_DIR) not in sys.path:
    sys.path.insert(0, str(ROOT_DIR))

from app.core.config import Settings
from app.db.json_repository import JsonAnalysisStore
from app.db.repository import to_analysis_dto
from app.models.schemas import ErrorSpan, TraceEventPayload
from app.redis.publisher import AnalysisPublisher
from app.services.analysis_service import AnalysisService
from app.services.llm_service import LLMService


def build_payload(trace_id: str, alert_id: str | None) -> tuple[TraceEventPayload, str | None]:
    now = datetime.now(UTC)
    return (
        TraceEventPayload(
            traceId=trace_id,
            rootService="checkout-service",
            rootOperation="POST /checkout",
            status="ERROR",
            failureType="DB_TIMEOUT",
            durationMs=5200,
            startedAt=now,
            endedAt=now + timedelta(milliseconds=5200),
            spanCount=4,
            errorSpans=[
                ErrorSpan(
                    spanId="span-payment-db",
                    serviceName="payment-service",
                    operation="SELECT payment_method",
                    errorMessage="database query timed out after 3000ms",
                    durationMs=3012,
                )
            ],
        ),
        alert_id,
    )


def settings_for_run(args: argparse.Namespace) -> Settings:
    updates: dict[str, object] = {
        "storage_backend": "json",
        "json_database_path": args.json_path,
        "redis_publish_enabled": args.publish_redis,
    }
    if args.provider:
        updates["llm_provider"] = args.provider
    return Settings(**updates)


async def run(args: argparse.Namespace) -> int:
    settings = settings_for_run(args)
    payload, alert_id = build_payload(args.trace_id, args.alert_id)
    store = JsonAnalysisStore(settings.json_database_path)
    llm_service = LLMService(settings)
    publisher = AnalysisPublisher(settings)
    service = AnalysisService(
        repository_factory=store.repository,
        llm_service=llm_service,
        publisher=publisher,
    )

    try:
        await service.process_trace_event(payload, alert_id)
        async with store.repository() as repository:
            row = await repository.get_by_trace_id(payload.trace_id)
        if row is None:
            print("No analysis row was created.", file=sys.stderr)
            return 1
        dto = to_analysis_dto(row)
        print(json.dumps(dto.model_dump(mode="json", by_alias=True), indent=2))
        return 0
    finally:
        await llm_service.close()
        await publisher.close()


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Run a mock trace through the full analysis flow without Kafka."
    )
    parser.add_argument("--trace-id", default="mock-trace-001")
    parser.add_argument("--alert-id", default="mock-alert-001")
    parser.add_argument("--json-path", default="data/ai_analysis.json")
    parser.add_argument("--provider", choices=("stub", "opencode"), default="stub")
    parser.add_argument("--publish-redis", action="store_true")
    return parser.parse_args()


if __name__ == "__main__":
    raise SystemExit(asyncio.run(run(parse_args())))
