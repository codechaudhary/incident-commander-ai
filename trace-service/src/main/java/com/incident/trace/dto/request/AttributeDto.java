package com.incident.trace.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttributeDto {

    private String key;

    private AttributeValueDto value;
}