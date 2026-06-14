package com.incident.alert.model.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class TraceIngestedEvent {
    private String eventId;
    private String eventType;
    private String eventVersion;
    private String timestamp;
    private String source;
    private TraceIngestedPayload payload;
}
