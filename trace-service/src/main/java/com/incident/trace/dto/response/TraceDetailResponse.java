package com.incident.trace.dto.response;

import com.incident.trace.enums.FailureType;
import com.incident.trace.enums.TraceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        name = "TraceDetailResponse",
        description = "Complete trace details including spans"
)
public class TraceDetailResponse {

    @Schema(
            description = "Unique trace identifier",
            example = "trace-e2e-001"
    )
    private String traceId;

    @Schema(
            description = "Root service name",
            example = "order-service"
    )
    private String rootService;

    @Schema(
            description = "Root operation",
            example = "create-order"
    )
    private String rootOperation;

    @Schema(
            description = "Overall trace status"
    )
    private TraceStatus status;

    @Schema(
            description = "Failure type if trace failed"
    )
    private FailureType failureType;

    @Schema(
            description = "Total duration in milliseconds",
            example = "10000"
    )
    private Long durationMs;

    @Schema(
            description = "Record creation timestamp"
    )
    private Instant createdAt;

    @Schema(
            description = "Trace start time"
    )
    private Instant startedAt;

    @Schema(
            description = "Trace end time"
    )
    private Instant endedAt;

    @Schema(
            description = "All spans belonging to this trace"
    )
    private List<SpanResponse> spans;
}