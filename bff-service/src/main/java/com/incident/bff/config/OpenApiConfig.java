package com.incident.bff.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bffServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BFF Service API")
                        .description("Backend-For-Frontend aggregating Trace, Alert, and AI services.")
                        .version("v1.0.0"));
    }
}
