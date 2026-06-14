package com.incident.trace.event;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TraceIngestedEvent {

    private String eventId;

    private String eventType;

    private String eventVersion;

    private Instant timestamp;

    private String source;

    private TraceIngestedPayload payload;
}