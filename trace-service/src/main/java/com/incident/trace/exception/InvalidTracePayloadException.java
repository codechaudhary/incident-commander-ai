package com.incident.trace.exception;

public class InvalidTracePayloadException extends RuntimeException {

    public InvalidTracePayloadException(String message) {
        super(message);
    }
}