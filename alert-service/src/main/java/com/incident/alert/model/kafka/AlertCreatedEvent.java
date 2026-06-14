package com.incident.alert.model.kafka;

import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertCreatedEvent {
    private String eventId;
    private String eventType;
    private String eventVersion;
    private Instant timestamp;
    private String source;
    private AlertCreatedPayload payload;
}
