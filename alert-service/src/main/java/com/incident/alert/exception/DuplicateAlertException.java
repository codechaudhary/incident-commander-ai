package com.incident.alert.exception;

public class DuplicateAlertException extends RuntimeException {
    public DuplicateAlertException(String traceId) {
        super("Alert already exists for traceId: " + traceId);
    }
}
