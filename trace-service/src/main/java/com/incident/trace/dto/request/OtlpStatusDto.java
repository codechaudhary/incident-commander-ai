package com.incident.trace.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtlpStatusDto {

    private Integer code;

    private String message;
}