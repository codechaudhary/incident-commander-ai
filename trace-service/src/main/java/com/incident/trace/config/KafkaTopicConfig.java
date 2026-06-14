package com.incident.trace.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic traceIngestedTopic() {

        return new NewTopic("trace-ingested-topic", 3, (short) 1);
    }
}