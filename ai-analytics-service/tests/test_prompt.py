from datetime import UTC, datetime

from app.models.schemas import ErrorSpan, TraceEventPayload
from app.prompts.root_cause_prompt import build_user_prompt


def test_prompt_contains_frozen_contract_fields() -> None:
    now = datetime.now(UTC)
    payload = TraceEventPayload(
        traceId="trace-1",
        rootService="order-service",
        rootOperation="POST /orders",
        status="ERROR",
        failureType="DB_TIMEOUT",
        durationMs=5200,
        startedAt=now,
        endedAt=now,
        spanCount=1,
        errorSpans=[
            ErrorSpan(
                spanId="span-1",
                serviceName="payment-service",
                operation="POST /charge",
                errorMessage="DB timeout",
                durationMs=5100,
            )
        ],
    )

    prompt = build_user_prompt(payload)

    assert "Trace ID: trace-1" in prompt
    assert "Root Service: order-service" in prompt
    assert "Failure Type: DB_TIMEOUT" in prompt
    assert '"serviceName": "payment-service"' in prompt
