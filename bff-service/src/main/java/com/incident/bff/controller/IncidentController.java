package com.incident.bff.controller;

import com.incident.bff.dto.IncidentViewResponse;
import com.incident.bff.service.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/incidents")
@Tag(name = "Incident API", description = "Aggregated incident views combining traces, alerts, and AI analysis")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping(value = "/{traceId}", produces = "application/json")
    @Operation(summary = "Get Incident View", description = "Fetches complete incident context for a given trace ID.")
    public Mono<IncidentViewResponse> getIncident(
            @Parameter(description = "Trace ID", example = "trace-123")
            @PathVariable String traceId) {
        return incidentService.getIncident(traceId);
    }
}
