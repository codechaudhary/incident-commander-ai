package com.incident.trace.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtlpEventDto {

    @NotBlank
    private String name;

    @NotNull
    private Long timeUnixNano;

    @Valid
    private List<AttributeDto> attributes;

}