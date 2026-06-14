package com.incident.trace.service;

import com.incident.trace.dto.response.PagedResponse;
import com.incident.trace.dto.response.TraceDetailResponse;
import com.incident.trace.dto.response.TraceSummaryResponse;
import com.incident.trace.enums.TraceStatus;

import java.time.Instant;

public interface TraceQueryService {

    TraceDetailResponse getTraceByTraceId(
            String traceId
    );

    PagedResponse<TraceSummaryResponse> getTraces(
            TraceStatus status,
            Instant from,
            Instant to,
            int page,
            int size
    );
}