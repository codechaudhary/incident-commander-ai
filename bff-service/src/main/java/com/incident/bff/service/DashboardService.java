package com.incident.bff.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.incident.bff.client.AiClient;
import com.incident.bff.client.AlertClient;
import com.incident.bff.client.TraceClient;
import com.incident.bff.dto.DashboardSummaryResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
public class DashboardService {

    private final TraceClient traceClient;
    private final AlertClient alertClient;
    private final AiClient aiClient;

    public DashboardService(TraceClient traceClient, AlertClient alertClient, AiClient aiClient) {
        this.traceClient = traceClient;
        this.alertClient = alertClient;
        this.aiClient = aiClient;
    }

    public Mono<DashboardSummaryResponse> getSummary() {
        Mono<Long> totalTracesMono = getCount(traceClient.getTraces("size=1"));
        Mono<Long> errorTracesMono = getCount(traceClient.getTraces("status=ERROR&size=1"));
        Mono<Long> openAlertsMono = getCount(alertClient.getAlerts("status=OPEN&size=1"));
        Mono<Long> criticalAlertsMono = getCount(alertClient.getAlerts("severity=CRITICAL&size=1"));
        Mono<Long> completedAnalysesMono = getCount(aiClient.getAnalyses("status=COMPLETED&size=1"));
        
        Mono<Long> avgDurationMono = traceClient.getTraces("size=20")
                .map(node -> {
                    if (node != null && node.has("content") && node.get("content").isArray() && !node.get("content").isEmpty()) {
                        long totalDuration = 0;
                        int count = 0;
                        for (JsonNode traceNode : node.get("content")) {
                            if (traceNode.has("durationMs")) {
                                totalDuration += traceNode.get("durationMs").asLong();
                                count++;
                            }
                        }
                        return count > 0 ? totalDuration / count : 0L;
                    }
                    return 0L;
                });

        return Mono.zip(
                totalTracesMono, errorTracesMono, openAlertsMono, 
                criticalAlertsMono, completedAnalysesMono, avgDurationMono
        ).map(tuple -> {
            DashboardSummaryResponse response = new DashboardSummaryResponse();
            response.setTotalTraces(tuple.getT1());
            response.setErrorTraces(tuple.getT2());
            response.setOpenAlerts(tuple.getT3());
            response.setCriticalAlerts(tuple.getT4());
            response.setCompletedAnalyses(tuple.getT5());
            response.setAvgTraceDurationMs(tuple.getT6());
            response.setLastUpdated(Instant.now());
            return response;
        });
    }

    private Mono<Long> getCount(Mono<JsonNode> responseMono) {
        return responseMono.map(node -> {
            if (node != null && node.has("totalElements")) {
                return node.get("totalElements").asLong();
            }
            return 0L;
        }).defaultIfEmpty(0L);
    }
}
