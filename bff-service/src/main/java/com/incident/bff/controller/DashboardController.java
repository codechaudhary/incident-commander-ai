package com.incident.bff.controller;

import com.incident.bff.dto.DashboardSummaryResponse;
import com.incident.bff.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard API", description = "Aggregated statistics for the frontend dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping(value = "/summary", produces = "application/json")
    @Operation(summary = "Get Dashboard Summary", description = "Aggregates total counts and stats from trace, alert, and AI services.")
    public Mono<DashboardSummaryResponse> getSummary() {
        return dashboardService.getSummary();
    }
}
