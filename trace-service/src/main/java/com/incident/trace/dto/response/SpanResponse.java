package com.incident.trace.dto.response;

import com.incident.trace.enums.SpanStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        name = "SpanResponse",
        description = "Details of an individual span within a distributed trace"
)
public class SpanResponse {

    @Schema(
            description = "Unique span identifier",
            example = "payment-span"
    )
    private String spanId;

    @Schema(
            description = "Parent span identifier. Null for root span",
            example = "root-span",
            nullable = true
    )
    private String parentSpanId;

    @Schema(
            description = "Service that generated the span",
            example = "order-service"
    )
    private String serviceName;

    @Schema(
            description = "Operation executed by the service",
            example = "charge-card"
    )
    private String operation;

    @Schema(
            description = "Execution status of the span"
    )
    private SpanStatus status;

    @Schema(
            description = "Span duration in milliseconds",
            example = "3500"
    )
    private Long durationMs;

    @Schema(
            description = "Span start timestamp"
    )
    private Instant startedAt;

    @Schema(
            description = "Span end timestamp"
    )
    private Instant endedAt;

    @Schema(
            description = "OpenTelemetry attributes associated with the span",
            example = """
                    {
                      "http.method":"POST",
                      "http.status_code":500,
                      "failure.type":"DB_TIMEOUT"
                    }
                    """
    )
    private Map<String, Object> attributes;

    @Schema(
            description = "OpenTelemetry events generated during span execution"
    )
    private List<Map<String, Object>> events;
}