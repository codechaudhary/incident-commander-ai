package com.incident.trace.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttributeValueDto {

    private String stringValue;

    private Long intValue;

    private Double doubleValue;

    private Boolean boolValue;
}