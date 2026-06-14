package com.incident.alert.kafka;

import com.incident.alert.mapper.AlertMapper;
import com.incident.alert.model.entity.AlertEntity;
import com.incident.alert.model.kafka.AlertCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertEventProducer {
    private final KafkaTemplate<String, AlertCreatedEvent> kafkaTemplate;
    private final AlertMapper alertMapper;

    @Value("${alert.kafka.topics.alert-events}")
    private String alertEventsTopic;

    @Async
    public void publishAlertCreated(AlertEntity entity) {
        try {
            AlertCreatedEvent event = alertMapper.toKafkaEvent(entity);
            kafkaTemplate.send(alertEventsTopic, entity.getTraceId(), event);
            log.info("Published alert event to Kafka. topic={} alertId={} traceId={}", alertEventsTopic, entity.getAlertId(), entity.getTraceId());
        } catch (Exception e) {
            log.error("Failed to publish alert event to Kafka. alertId={} error={}", entity.getAlertId(), e.getMessage(), e);
        }
    }
}
