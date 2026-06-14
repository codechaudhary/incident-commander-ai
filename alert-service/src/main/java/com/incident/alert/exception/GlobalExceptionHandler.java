package com.incident.alert.exception;

import com.incident.alert.model.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AlertNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(AlertNotFoundException ex, HttpServletRequest req) {
        log.warn("Alert not found. path={} message={}", req.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), req);
    }

    @ExceptionHandler(InvalidAlertStatusException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStatus(InvalidAlertStatusException ex, HttpServletRequest req) {
        log.warn("Invalid alert status transition. path={} message={}", req.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), req);
    }

    @ExceptionHandler(DuplicateAlertException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateAlertException ex, HttpServletRequest req) {
        log.warn("Duplicate alert detected. path={} message={}", req.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getField() + " " + e.getDefaultMessage())
                .orElse("Validation failed");
        log.warn("Validation failed. path={} message={}", req.getRequestURI(), message);
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation Error", message, req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        String message = "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'";
        log.warn("Type mismatch. path={} message={}", req.getRequestURI(), message);
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", message, req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        log.warn("Malformed request body. path={}", req.getRequestURI());
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", "Malformed request body", req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.error("Data integrity violation. path={} cause={}", req.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return buildResponse(HttpStatus.CONFLICT, "Conflict", "Database constraint violation", req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error. path={} type={} message={}", req.getRequestURI(), ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred", req);
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String error, String message, HttpServletRequest req) {
        return ResponseEntity.status(status).body(ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(error)
                .message(message)
                .path(req.getRequestURI())
                .traceId(null)
                .build());
    }
}
