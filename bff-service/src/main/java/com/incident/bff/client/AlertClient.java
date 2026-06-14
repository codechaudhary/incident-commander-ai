package com.incident.bff.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class AlertClient {

    private static final Logger log = LoggerFactory.getLogger(AlertClient.class);
    private final WebClient alertWebClient;

    public AlertClient(WebClient alertWebClient) {
        this.alertWebClient = alertWebClient;
    }

    public Mono<JsonNode> getAlertByTraceId(String traceId) {
        return alertWebClient.get()
                .uri("/api/v1/alerts?traceId={traceId}", traceId)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(node -> {
                    if (node.has("content") && node.get("content").isArray() && !node.get("content").isEmpty()) {
                        return node.get("content").get(0);
                    }
                    return null;
                })
                .onErrorResume(e -> {
                    log.error("Failed to fetch alert for trace {} from alert-service", traceId, e);
                    return Mono.empty();
                });
    }

    public Mono<JsonNode> getAlerts(String queryParams) {
        return alertWebClient.get()
                .uri("/api/v1/alerts" + (queryParams.isEmpty() ? "" : "?" + queryParams))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .onErrorResume(e -> {
                    log.error("Failed to fetch alerts from alert-service", e);
                    return Mono.empty();
                });
    }
}
