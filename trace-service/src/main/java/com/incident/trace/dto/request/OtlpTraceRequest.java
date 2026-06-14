package com.incident.trace.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtlpTraceRequest {

    @NotEmpty
    @Valid
    private List<ResourceSpanDto> resourceSpans;
}