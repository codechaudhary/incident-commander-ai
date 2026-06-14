package com.incident.trace.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "trace.kafka.topics")
public class KafkaTopicProperties {

    private String traceIngested;
}