package com.incident.bff.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.incident.bff.client.AlertClient;
import com.incident.bff.client.TraceClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Proxy API", description = "Pass-through APIs for paginated lists")
public class ProxyController {

    private final TraceClient traceClient;
    private final AlertClient alertClient;

    public ProxyController(TraceClient traceClient, AlertClient alertClient) {
        this.traceClient = traceClient;
        this.alertClient = alertClient;
    }

    @GetMapping(value = "/traces", produces = "application/json")
    @Operation(summary = "Proxy to GET /api/v1/traces", description = "Passes query parameters down to trace-service.")
    public Mono<JsonNode> getTraces(ServerHttpRequest request) {
        String query = request.getURI().getQuery();
        return traceClient.getTraces(query == null ? "" : query);
    }

    @GetMapping(value = "/alerts", produces = "application/json")
    @Operation(summary = "Proxy to GET /api/v1/alerts", description = "Passes query parameters down to alert-service.")
    public Mono<JsonNode> getAlerts(ServerHttpRequest request) {
        String query = request.getURI().getQuery();
        return alertClient.getAlerts(query == null ? "" : query);
    }
}
