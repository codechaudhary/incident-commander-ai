package com.incident.trace.event;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class TraceIngestedPayload {

    private String traceId;

    private String rootService;

    private String rootOperation;

    private String status;

    private String failureType;

    private Long durationMs;

    private Instant startedAt;

    private Instant endedAt;

    private Integer spanCount;

    private List<ErrorSpanPayload> errorSpans;
}