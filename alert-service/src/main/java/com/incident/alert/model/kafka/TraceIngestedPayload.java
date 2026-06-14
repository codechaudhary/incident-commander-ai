package com.incident.alert.model.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class TraceIngestedPayload {
    private String traceId;
    private String rootService;
    private String rootOperation;
    private String status;
    private String failureType;
    private Long durationMs;
    private String startedAt;
    private String endedAt;
    private Integer spanCount;
    private List<ErrorSpanPayload> errorSpans;
}
