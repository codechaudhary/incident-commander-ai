package com.incident.bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${app.services.trace.url}")
    private String traceServiceUrl;

    @Value("${app.services.alert.url}")
    private String alertServiceUrl;

    @Value("${app.services.ai.url}")
    private String aiServiceUrl;

    @Bean
    public WebClient traceWebClient(WebClient.Builder builder) {
        return builder.baseUrl(traceServiceUrl).build();
    }

    @Bean
    public WebClient alertWebClient(WebClient.Builder builder) {
        return builder.baseUrl(alertServiceUrl).build();
    }

    @Bean
    public WebClient aiWebClient(WebClient.Builder builder) {
        return builder.baseUrl(aiServiceUrl).build();
    }
}
