from datetime import UTC, datetime
from uuid import uuid4

from app.models.schemas import KafkaEnvelope, LLMAnalysisResult


def test_trace_event_contract_accepts_camel_case_payload() -> None:
    now = datetime.now(UTC).isoformat().replace("+00:00", "Z")
    envelope = KafkaEnvelope.model_validate(
        {
            "eventId": str(uuid4()),
            "eventType": "TRACE_INGESTED",
            "eventVersion": "1.0.0",
            "timestamp": now,
            "source": "trace-service",
            "payload": {
                "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
                "rootService": "order-service",
                "rootOperation": "POST /orders",
                "status": "ERROR",
                "failureType": "DB_TIMEOUT",
                "durationMs": 5200,
                "startedAt": now,
                "endedAt": now,
                "spanCount": 1,
                "errorSpans": [
                    {
                        "spanId": "a2fb4a1d1a96d312",
                        "serviceName": "payment-service",
                        "operation": "POST /charge",
                        "errorMessage": "DB timeout",
                        "durationMs": 5100,
                    }
                ],
            },
        }
    )

    assert envelope.payload.trace_id == "4bf92f3577b34da6a3ce929d0e0e4736"
    assert envelope.payload.error_spans[0].service_name == "payment-service"


def test_llm_response_contract_serializes_to_camel_case() -> None:
    result = LLMAnalysisResult(
        root_cause="Payment database timed out.",
        affected_services=["order-service", "payment-service"],
        recommendations=["Increase DB pool size."],
        confidence_score=0.87,
    )

    assert result.model_dump(by_alias=True) == {
        "rootCause": "Payment database timed out.",
        "affectedServices": ["order-service", "payment-service"],
        "recommendations": ["Increase DB pool size."],
        "confidenceScore": 0.87,
    }
