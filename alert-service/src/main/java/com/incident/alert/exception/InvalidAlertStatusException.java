package com.incident.alert.exception;

public class InvalidAlertStatusException extends RuntimeException {
    public InvalidAlertStatusException(String message) {
        super(message);
    }
}
