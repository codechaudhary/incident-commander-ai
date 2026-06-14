package com.incident.alert.controller;

import com.incident.alert.model.dto.AlertResponse;
import com.incident.alert.model.dto.AlertStatusUpdateRequest;
import com.incident.alert.model.dto.PagedResponse;
import com.incident.alert.model.enums.AlertSeverity;
import com.incident.alert.model.enums.AlertStatus;
import com.incident.alert.service.AlertQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Tag(name = "Alert APIs", description = "Alert lifecycle management APIs")
public class AlertController {
    private final AlertQueryService alertQueryService;

    @GetMapping(produces = "application/json")
    @Operation(summary = "List alerts", description = "Search alerts with optional status, severity, and traceId filters.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Alerts retrieved"))
    public PagedResponse<AlertResponse> getAlerts(
            @Parameter(description = "Filter by alert status", example = "OPEN")
            @RequestParam(required = false) AlertStatus status,
            @Parameter(description = "Filter by severity", example = "CRITICAL")
            @RequestParam(required = false) AlertSeverity severity,
            @Parameter(description = "Filter by trace ID")
            @RequestParam(required = false) String traceId,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Page size (max 100)", example = "20")
            @RequestParam(defaultValue = "20") Integer size) {
        return alertQueryService.getAlerts(status, severity, traceId, page, Math.min(size, 100));
    }

    @GetMapping(value = "/{alertId}", produces = "application/json")
    @Operation(summary = "Get alert by alertId", description = "Returns a single alert by its unique alert ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alert found"),
            @ApiResponse(responseCode = "404", description = "Alert not found")
    })
    public AlertResponse getAlertById(
            @Parameter(description = "Unique alert identifier", example = "ALT-a1b2c3d4")
            @PathVariable String alertId) {
        return alertQueryService.getAlertByAlertId(alertId);
    }

    @PatchMapping(value = "/{alertId}/status", consumes = "application/json", produces = "application/json")
    @Operation(summary = "Update alert status", description = "Transition alert to ACKNOWLEDGED or RESOLVED. Cannot revert to OPEN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "404", description = "Alert not found")
    })
    public ResponseEntity<AlertResponse> updateStatus(
            @Parameter(description = "Unique alert identifier")
            @PathVariable String alertId,
            @Valid @RequestBody AlertStatusUpdateRequest request) {
        return ResponseEntity.ok(alertQueryService.updateAlertStatus(alertId, request.getStatus()));
    }
}
