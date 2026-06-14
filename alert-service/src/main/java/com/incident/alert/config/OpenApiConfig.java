package com.incident.alert.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI alertServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("AI Incident Commander — Alert Service API")
                .description("Alert lifecycle management: creation from trace events, querying, and status updates.")
                .version("v1")
                .contact(new Contact().name("Harshit Chaudhary")));
    }
}
