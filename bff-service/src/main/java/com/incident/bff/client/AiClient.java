package com.incident.bff.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

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
                .onErrorResume(org.springframework.web.reactive.function.client.WebClientResponseException.NotFound.class, e -> Mono.empty())
                .onErrorResume(e -> {
                    log.warn("Failed to fetch analysis for trace {} from ai-service: {}", traceId, e.getMessage());
                    return Mono.empty();
                });
    }

    /**
     * Trigger LLM analysis for a trace via the ai-analytics-service REST API.
     * Returns the pending response (202) or empty on error.
     */
    public Mono<JsonNode> triggerAnalysis(String traceId, JsonNode traceData) {
        return aiWebClient.post()
                .uri("/api/v1/analyses/trigger")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "traceId", traceId,
                        "rootService", traceData.path("rootService").asText("unknown"),
                        "rootOperation", traceData.path("rootOperation").asText("unknown"),
                        "status", traceData.path("status").asText("ERROR"),
                        "failureType", traceData.path("failureType").asText("NONE"),
                        "durationMs", traceData.path("durationMs").asInt(1),
                        "errorSpans", traceData.path("spans") // pass spans as errorSpans
                ))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .onErrorResume(e -> {
                    log.warn("Failed to trigger analysis for trace {}: {}", traceId, e.getMessage());
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
