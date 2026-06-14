package com.incident.trace.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI traceServiceOpenApi() {

        return new OpenAPI()
                .info(
                        new Info()
                                .title(
                                        "AI Incident Commander - Trace Service API"
                                )
                                .description(
                                        "Distributed tracing ingestion and query APIs"
                                )
                                .version(
                                        "v1"
                                )
                                .contact(
                                        new Contact()
                                                .name(
                                                        "Harshit Chaudhary"
                                                )
                                )
                                .license(
                                        new License()
                                                .name(
                                                        "Internal"
                                                )
                                )
                );
    }
}