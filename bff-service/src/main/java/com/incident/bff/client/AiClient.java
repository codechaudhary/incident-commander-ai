package com.incident.bff.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class AiClient {

    private static final Logger log = LoggerFactory.getLogger(AiClient.class);
    private final WebClient aiWebClient;

    public AiClient(WebClient aiWebClient) {
        this.aiWebClient = aiWebClient;
    }

    public Mono<JsonNode> getAnalysisByTraceId(String traceId) {
        return aiWebClient.get()
                .uri("/api/v1/analyses/{traceId}", traceId)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .onErrorResume(e -> {
                    log.error("Failed to fetch analysis for trace {} from ai-service", traceId, e);
                    return Mono.empty();
                });
    }

    public Mono<JsonNode> getAnalyses(String queryParams) {
        return aiWebClient.get()
                .uri("/api/v1/analyses" + (queryParams.isEmpty() ? "" : "?" + queryParams))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .onErrorResume(e -> {
                    log.error("Failed to fetch analyses from ai-service", e);
                    return Mono.empty();
                });
    }
}
