package com.incident.trace.event;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorSpanPayload {

    private String spanId;

    private String serviceName;

    private String operation;

    private String errorMessage;

    private Long durationMs;
}