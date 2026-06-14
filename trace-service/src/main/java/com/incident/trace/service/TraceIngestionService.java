package com.incident.trace.service;

import com.incident.trace.dto.request.OtlpTraceRequest;
import com.incident.trace.dto.response.TraceIngestResponse;

public interface TraceIngestionService {

    TraceIngestResponse ingestTrace(
            OtlpTraceRequest request
    );
}