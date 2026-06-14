package com.incident.bff.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class TraceClient {

    private static final Logger log = LoggerFactory.getLogger(TraceClient.class);
    private final WebClient traceWebClient;

    public TraceClient(WebClient traceWebClient) {
        this.traceWebClient = traceWebClient;
    }

    public Mono<JsonNode> getTraceById(String traceId) {
        return traceWebClient.get()
                .uri("/api/v1/traces/{traceId}", traceId)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .onErrorResume(e -> {
                    log.error("Failed to fetch trace {} from trace-service", traceId, e);
                    return Mono.empty();
                });
    }

    public Mono<JsonNode> getTraces(String queryParams) {
        return traceWebClient.get()
                .uri("/api/v1/traces" + (queryParams.isEmpty() ? "" : "?" + queryParams))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .onErrorResume(e -> {
                    log.error("Failed to fetch traces from trace-service", e);
                    return Mono.empty();
                });
    }
}
