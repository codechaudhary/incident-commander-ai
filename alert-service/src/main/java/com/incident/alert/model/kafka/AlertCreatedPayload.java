package com.incident.alert.model.kafka;

import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertCreatedPayload {
    private String alertId;
    private String traceId;
    private String severity;
    private String status;
    private String title;
    private String description;
    private Instant triggeredAt;
}
