package com.incident.trace.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtlpSpanDto {

    @NotBlank
    private String traceId;

    @NotBlank
    private String spanId;

    private String parentSpanId;

    @NotBlank
    private String name;

    @NotNull
    private String startTimeUnixNano;

    @NotNull
    private String endTimeUnixNano;

    private List<AttributeDto> attributes;

    private List<OtlpEventDto> events;

    private OtlpStatusDto status;
}