package com.incident.trace.exception;

public class DuplicateTraceException
        extends RuntimeException {

    public DuplicateTraceException(
            String traceId
    ) {
        super(
                "Trace already exists: "
                        + traceId
        );
    }
}