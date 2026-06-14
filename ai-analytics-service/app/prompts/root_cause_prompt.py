import json

from app.models.schemas import TraceEventPayload

SYSTEM_PROMPT = """You are an expert Site Reliability Engineer (SRE) analyzing distributed
system traces. You will be given a trace from an OpenTelemetry-instrumented system
that has experienced an error or timeout.
Analyze the trace and provide a concise root cause analysis.

Respond ONLY in the following JSON format, with no additional text:
{
  "rootCause": "string (1-3 sentences explaining what failed and why)",
  "affectedServices": ["array of service names"],
  "recommendations": ["array of 2-4 actionable recommendations"],
  "confidenceScore": 0.0-1.0
}
"""


def build_user_prompt(payload: TraceEventPayload) -> str:
    error_spans_json = json.dumps(
        [span.model_dump(by_alias=True) for span in payload.error_spans],
        indent=2,
        default=str,
    )
    span_summary_json = json.dumps(
        {
            "traceId": payload.trace_id,
            "rootService": payload.root_service,
            "rootOperation": payload.root_operation,
            "status": payload.status,
            "durationMs": payload.duration_ms,
            "spanCount": payload.span_count,
            "startedAt": payload.started_at.isoformat(),
            "endedAt": payload.ended_at.isoformat(),
        },
        indent=2,
    )

    return f"""Trace ID: {payload.trace_id}
Root Service: {payload.root_service}
Operation: {payload.root_operation}
Status: {payload.status}
Total Duration: {payload.duration_ms}ms
Failure Type: {payload.failure_type}

Error Spans:
{error_spans_json}

All Span Summary:
{span_summary_json}

Analyze this trace and identify the root cause."""
