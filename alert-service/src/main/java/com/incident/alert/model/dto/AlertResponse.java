package com.incident.alert.model.dto;

import com.incident.alert.model.enums.AlertSeverity;
import com.incident.alert.model.enums.AlertStatus;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class AlertResponse {
    private UUID id;
    private String alertId;
    private String traceId;
    private AlertSeverity severity;
    private AlertStatus status;
    private String title;
    private String description;
    private Instant triggeredAt;
    private Instant updatedAt;
}
