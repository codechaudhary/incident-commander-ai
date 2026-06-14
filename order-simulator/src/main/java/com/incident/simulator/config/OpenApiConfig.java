package com.incident.simulator.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI simulatorOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Simulator API")
                        .description("API for simulating incoming orders and failures")
                        .version("1.0.0"));
    }
}
