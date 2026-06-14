package com.incident.alert.model.dto;

import com.incident.alert.model.enums.AlertStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlertStatusUpdateRequest {
    @NotNull(message = "Status is required")
    private AlertStatus status;
}
