package com.incident.bff.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incident.bff.client.AiClient;
import com.incident.bff.client.AlertClient;
import com.incident.bff.client.TraceClient;
import com.incident.bff.dto.IncidentViewResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Service
public class IncidentService {

    private final TraceClient traceClient;
    private final AlertClient alertClient;
    private final AiClient aiClient;

    public IncidentService(TraceClient traceClient, AlertClient alertClient, AiClient aiClient) {
        this.traceClient = traceClient;
        this.alertClient = alertClient;
        this.aiClient = aiClient;
    }

    public Mono<IncidentViewResponse> getIncident(String traceId) {
        return traceClient.getTraceById(traceId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Trace not found")))
                .flatMap(traceNode -> {
                    Mono<JsonNode> alertMono = alertClient.getAlertByTraceId(traceId)
                            .defaultIfEmpty(new ObjectMapper().createObjectNode());

                    Mono<JsonNode> analysisMono = aiClient.getAnalysisByTraceId(traceId)
                            .switchIfEmpty(
                                aiClient.triggerAnalysis(traceId, traceNode)
                                    .defaultIfEmpty(new ObjectMapper().createObjectNode())
                            )
                            .defaultIfEmpty(new ObjectMapper().createObjectNode());

                    return Mono.zip(Mono.just(traceNode), alertMono, analysisMono);
                })
                .map(tuple -> new IncidentViewResponse(
                        tuple.getT1(),
                        tuple.getT2().isEmpty() ? null : tuple.getT2(),
                        tuple.getT3().isEmpty() ? null : tuple.getT3()
                ));
    }
}
