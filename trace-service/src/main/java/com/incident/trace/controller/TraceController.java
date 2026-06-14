package com.incident.trace.controller;

import com.incident.trace.dto.request.OtlpTraceRequest;
import com.incident.trace.dto.response.PagedResponse;
import com.incident.trace.dto.response.TraceDetailResponse;
import com.incident.trace.dto.response.TraceIngestResponse;
import com.incident.trace.dto.response.TraceSummaryResponse;
import com.incident.trace.enums.TraceStatus;
import com.incident.trace.service.TraceIngestionService;
import com.incident.trace.service.TraceQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/traces")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Trace APIs",
        description = "Distributed tracing ingestion and query APIs"
)
public class TraceController {

    private final TraceIngestionService traceIngestionService;

    private final TraceQueryService traceQueryService;

    @PostMapping(
            consumes = "application/json",
            produces = "application/json"
    )
    @Operation(
            summary = "Ingest trace",
            description =
                    "Ingests an OpenTelemetry trace payload, persists trace and span data, and publishes a Kafka event after successful database commit."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Trace ingested successfully",
                    content = @Content(
                            schema = @Schema(
                                    implementation = TraceIngestResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid trace payload"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Duplicate trace detected"
            )
    })
    public TraceIngestResponse ingestTrace(
            @Valid
            @RequestBody
            OtlpTraceRequest request
    ) {

        return traceIngestionService.ingestTrace(
                request
        );
    }

    @GetMapping(
            value = "/{traceId}",
            produces = "application/json"
    )
    @Operation(
            summary = "Get trace by traceId",
            description =
                    "Returns complete trace details including all spans, attributes, events, timings, and failure information."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Trace found",
                    content = @Content(
                            schema = @Schema(
                                    implementation = TraceDetailResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Trace not found"
            )
    })
    public TraceDetailResponse getTraceById(

            @Parameter(
                    description = "Unique trace identifier",
                    example = "trace-e2e-001"
            )
            @PathVariable
            String traceId
    ) {

        return traceQueryService.getTraceByTraceId(
                traceId
        );
    }

    @GetMapping(
            produces = "application/json"
    )
    @Operation(
            summary = "Search traces",
            description =
                    "Search traces using status and time range filters with pagination support."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Search completed successfully"
            )
    })
    public PagedResponse<TraceSummaryResponse> getTraces(

            @Parameter(
                    description = "Filter by trace status",
                    example = "ERROR"
            )
            @RequestParam(required = false)
            TraceStatus status,

            @Parameter(
                    description = "Start timestamp (ISO-8601 UTC)",
                    example = "2024-06-06T08:00:00Z"
            )
            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            Instant from,

            @Parameter(
                    description = "End timestamp (ISO-8601 UTC)",
                    example = "2024-06-06T09:00:00Z"
            )
            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            Instant to,

            @Parameter(
                    description = "Page number",
                    example = "0"
            )
            @RequestParam(defaultValue = "0")
            Integer page,

            @Parameter(
                    description = "Page size (max 100)",
                    example = "20"
            )
            @RequestParam(defaultValue = "20")
            Integer size
    ) {

        return traceQueryService.getTraces(
                status,
                from,
                to,
                page,
                Math.min(size, 100)
        );
    }
}