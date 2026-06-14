from __future__ import annotations
import json

from app.models.schemas import TraceEventPayload

SYSTEM_PROMPT = """You are an SRE analyzing distributed traces. Respond ONLY in this JSON format, no extra text:
{"rootCause":"1-2 sentence root cause","affectedServices":["service names"],"recommendations":["2-3 short actions"],"confidenceScore":0.0-1.0}"""


def build_user_prompt(payload: TraceEventPayload) -> str:
    # Build a compact summary - only include what the LLM needs
    error_info = []
    for span in payload.error_spans[:5]:  # cap at 5 spans
        error_info.append({
            "service": span.service_name,
            "op": span.operation,
            "err": span.error_message,
            "ms": span.duration_ms,
        })

    summary = {
        "traceId": payload.trace_id,
        "rootService": payload.root_service,
        "status": payload.status,
        "failureType": payload.failure_type,
        "durationMs": payload.duration_ms,
        "errorSpans": error_info,
    }

    return f"Trace: {json.dumps(summary, default=str)}\nIdentify root cause."
