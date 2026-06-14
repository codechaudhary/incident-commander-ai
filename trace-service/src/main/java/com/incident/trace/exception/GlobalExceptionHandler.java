package com.incident.trace.exception;

import com.incident.trace.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(
            TraceNotFoundException.class
    )
    public ResponseEntity<ErrorResponse> handleTraceNotFound(
            TraceNotFoundException exception,
            HttpServletRequest request
    ) {

        ErrorResponse response =
                ErrorResponse.builder()
                        .timestamp(
                                Instant.now()
                        )
                        .status(
                                HttpStatus.NOT_FOUND.value()
                        )
                        .error(
                                "TRACE_NOT_FOUND"
                        )
                        .message(
                                exception.getMessage()
                        )
                        .path(
                                request.getRequestURI()
                        )
                        .build();

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    @ExceptionHandler(
            InvalidTracePayloadException.class
    )
    public ResponseEntity<ErrorResponse> handleInvalidPayload(
            InvalidTracePayloadException exception,
            HttpServletRequest request
    ) {

        ErrorResponse response =
                ErrorResponse.builder()
                        .timestamp(
                                Instant.now()
                        )
                        .status(
                                HttpStatus.BAD_REQUEST.value()
                        )
                        .error(
                                "INVALID_TRACE_PAYLOAD"
                        )
                        .message(
                                exception.getMessage()
                        )
                        .path(
                                request.getRequestURI()
                        )
                        .build();

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    @ExceptionHandler(
            MethodArgumentNotValidException.class
    )
    public ResponseEntity<ErrorResponse> handleValidationFailure(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {

        String message =
                exception.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .findFirst()
                        .map(error ->
                                error.getField()
                                        + " "
                                        + error.getDefaultMessage()
                        )
                        .orElse("Validation failed");

        ErrorResponse response =
                ErrorResponse.builder()
                        .timestamp(
                                Instant.now()
                        )
                        .status(
                                HttpStatus.BAD_REQUEST.value()
                        )
                        .error(
                                "VALIDATION_ERROR"
                        )
                        .message(
                                message
                        )
                        .path(
                                request.getRequestURI()
                        )
                        .build();

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    @ExceptionHandler(
            Exception.class
    )
    public ResponseEntity<ErrorResponse> handleUnexpectedError(
            Exception exception,
            HttpServletRequest request
    ) {

        ErrorResponse response =
                ErrorResponse.builder()
                        .timestamp(
                                Instant.now()
                        )
                        .status(
                                HttpStatus.INTERNAL_SERVER_ERROR.value()
                        )
                        .error(
                                "INTERNAL_SERVER_ERROR"
                        )
                        .message(
                                exception.getMessage()
                        )
                        .path(
                                request.getRequestURI()
                        )
                        .build();

        return ResponseEntity
                .status(
                        HttpStatus.INTERNAL_SERVER_ERROR
                )
                .body(response);
    }
    @ExceptionHandler(
            DuplicateTraceException.class
    )
    public ResponseEntity<ErrorResponse>
    handleDuplicateTrace(
            DuplicateTraceException exception,
            HttpServletRequest request
    ) {

        ErrorResponse response =
                ErrorResponse.builder()
                        .timestamp(
                                Instant.now()
                        )
                        .status(409)
                        .error(
                                "DUPLICATE_TRACE"
                        )
                        .message(
                                exception.getMessage()
                        )
                        .path(
                                request.getRequestURI()
                        )
                        .build();

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }
    @ExceptionHandler(
            DataIntegrityViolationException.class
    )
    public ResponseEntity<ErrorResponse>
    handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {

        ErrorResponse response =
                ErrorResponse.builder()
                        .timestamp(
                                Instant.now()
                        )
                        .status(409)
                        .error(
                                "DATABASE_CONSTRAINT_VIOLATION"
                        )
                        .message(
                                exception.getMostSpecificCause()
                                        .getMessage()
                        )
                        .path(
                                request.getRequestURI()
                        )
                        .build();

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }
}