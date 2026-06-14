package com.incident.alert.model.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrorSpanPayload {
    private String spanId;
    private String serviceName;
    private String operation;
    private String errorMessage;
    private Long durationMs;
}
