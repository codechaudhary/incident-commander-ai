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
public class ResourceSpanDto {

    private ResourceDto resource;

    @Valid
    @NotEmpty
    private List<ScopeSpanDto> scopeSpans;
}