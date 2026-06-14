package com.incident.trace.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ErrorResponse {

    private Instant timestamp;

    private Integer status;

    private String error;

    private String message;

    private String path;

    private String traceId;
}