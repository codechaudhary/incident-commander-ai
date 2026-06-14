package com.incident.simulator.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class TraceClient {

    private static final Logger log = LoggerFactory.getLogger(TraceClient.class);
    private final WebClient webClient;

    public TraceClient(WebClient.Builder webClientBuilder, @Value("${app.services.trace-url}") String traceUrl) {
        this.webClient = webClientBuilder.baseUrl(traceUrl).build();
    }

    public Mono<Void> sendTrace(JsonNode tracePayload) {
        return webClient.post()
                .uri("/api/v1/traces")
                .bodyValue(tracePayload)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.info("Successfully sent trace to trace-service"))
                .doOnError(e -> log.error("Failed to send trace to trace-service: {}", e.getMessage()));
    }
}
