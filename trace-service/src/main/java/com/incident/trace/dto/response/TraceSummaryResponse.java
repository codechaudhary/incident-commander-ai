package com.incident.trace.dto.response;

import com.incident.trace.enums.FailureType;
import com.incident.trace.enums.TraceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        name = "TraceSummaryResponse",
        description = "Summary information about a trace"
)
public class TraceSummaryResponse {

    @Schema(
            description = "Unique trace identifier",
            example = "trace-e2e-001"
    )
    private String traceId;

    @Schema(
            description = "Root service that initiated the trace",
            example = "order-service"
    )
    private String rootService;

    @Schema(
            description = "Root operation name",
            example = "create-order"
    )
    private String rootOperation;

    @Schema(
            description = "Overall trace status"
    )
    private TraceStatus status;

    @Schema(
            description = "Failure category if trace failed"
    )
    private FailureType failureType;

    @Schema(
            description = "Total trace duration in milliseconds",
            example = "10000"
    )
    private Long durationMs;

    @Schema(
            description = "Trace start time"
    )
    private Instant startedAt;

    @Schema(
            description = "Trace end time"
    )
    private Instant endedAt;

    @Schema(
            description = "Number of spans in the trace",
            example = "3"
    )
    private Integer spanCount;

    @Schema(
            description = "Database creation timestamp"
    )
    private Instant createdAt;
}