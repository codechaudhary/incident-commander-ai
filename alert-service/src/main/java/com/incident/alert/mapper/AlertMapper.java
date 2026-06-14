package com.incident.alert.mapper;

import com.incident.alert.model.dto.AlertResponse;
import com.incident.alert.model.dto.PagedResponse;
import com.incident.alert.model.entity.AlertEntity;
import com.incident.alert.model.enums.AlertSeverity;
import com.incident.alert.model.kafka.*;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class AlertMapper {
    private static final long CRITICAL_DURATION_THRESHOLD_MS = 5000L;
    private static final Map<String, AlertSeverity> FAILURE_SEVERITY = Map.of(
            "RUNTIME_EXCEPTION", AlertSeverity.CRITICAL,
            "DB_TIMEOUT", AlertSeverity.HIGH,
            "SLOW_RESPONSE", AlertSeverity.MEDIUM
    );

    public AlertSeverity determineSeverity(TraceIngestedPayload payload) {
        String failureType = payload.getFailureType();
        AlertSeverity base = FAILURE_SEVERITY.getOrDefault(failureType, AlertSeverity.HIGH);
        if ("DB_TIMEOUT".equals(failureType) && payload.getDurationMs() != null
                && payload.getDurationMs() > CRITICAL_DURATION_THRESHOLD_MS) {
            return AlertSeverity.CRITICAL;
        }
        return base;
    }

    public String buildTitle(TraceIngestedPayload payload) {
        String service = payload.getRootService() != null ? payload.getRootService() : "Unknown";
        String failureType = payload.getFailureType() != null
                ? payload.getFailureType().replace("_", " ") : "Error";
        return service + " — " + failureType;
    }

    public String buildDescription(TraceIngestedPayload payload) {
        StringBuilder sb = new StringBuilder();
        sb.append(payload.getRootService()).append(" failed at operation '")
          .append(payload.getRootOperation()).append("'. ");
        sb.append("Duration: ").append(payload.getDurationMs()).append("ms. ");
        sb.append("Failure type: ").append(payload.getFailureType()).append(". ");
        List<ErrorSpanPayload> errors = payload.getErrorSpans();
        if (errors != null && !errors.isEmpty()) {
            ErrorSpanPayload first = errors.getFirst();
            sb.append("Error in ").append(first.getServiceName())
              .append(" (").append(first.getOperation()).append("): ")
              .append(first.getErrorMessage()).append(".");
        }
        return sb.toString();
    }

    public AlertEntity toEntity(TraceIngestedPayload payload, AlertSeverity severity) {
        return AlertEntity.builder()
                .alertId("ALT-" + UUID.randomUUID().toString().substring(0, 8))
                .traceId(payload.getTraceId())
                .severity(severity)
                .title(buildTitle(payload))
                .description(buildDescription(payload))
                .build();
    }

    public AlertResponse toResponse(AlertEntity entity) {
        return AlertResponse.builder()
                .id(entity.getId())
                .alertId(entity.getAlertId())
                .traceId(entity.getTraceId())
                .severity(entity.getSeverity())
                .status(entity.getStatus())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .triggeredAt(entity.getTriggeredAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public <T> PagedResponse<T> toPagedResponse(Page<?> page, List<T> content) {
        return PagedResponse.<T>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    public AlertCreatedEvent toKafkaEvent(AlertEntity entity) {
        return AlertCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("ALERT_CREATED")
                .eventVersion("1.0.0")
                .timestamp(Instant.now())
                .source("alert-service")
                .payload(AlertCreatedPayload.builder()
                        .alertId(entity.getAlertId())
                        .traceId(entity.getTraceId())
                        .severity(entity.getSeverity().name())
                        .status(entity.getStatus().name())
                        .title(entity.getTitle())
                        .description(entity.getDescription())
                        .triggeredAt(entity.getTriggeredAt())
                        .build())
                .build();
    }

    public Map<String, Object> toRedisMessage(AlertEntity entity) {
        return Map.of(
                "type", "ALERT_CREATED",
                "alertId", entity.getAlertId(),
                "traceId", entity.getTraceId(),
                "severity", entity.getSeverity().name(),
                "status", entity.getStatus().name(),
                "title", entity.getTitle(),
                "description", entity.getDescription(),
                "triggeredAt", entity.getTriggeredAt().toString()
        );
    }
}
