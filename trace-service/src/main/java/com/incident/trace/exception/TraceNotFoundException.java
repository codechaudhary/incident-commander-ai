package com.incident.trace.exception;

public class TraceNotFoundException extends RuntimeException {

    public TraceNotFoundException(String traceId) {
        super("Trace not found: " + traceId);
    }
}