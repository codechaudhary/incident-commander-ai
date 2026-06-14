package com.incident.trace.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        name = "TraceIngestResponse",
        description = "Response returned after successful trace ingestion"
)
public class TraceIngestResponse {

    @Schema(
            description = "Unique trace identifier",
            example = "trace-e2e-001"
    )
    private String traceId;

    @Schema(
            description = "Ingestion status",
            example = "INGESTED"
    )
    private String status;

    @Schema(
            description = "Total number of spans stored",
            example = "3"
    )
    private Integer spanCount;
}