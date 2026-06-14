package com.incident.trace.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScopeSpanDto {

    @NotEmpty
    private List<OtlpSpanDto> spans;
}