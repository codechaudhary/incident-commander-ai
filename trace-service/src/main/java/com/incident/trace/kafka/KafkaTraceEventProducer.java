package com.incident.trace.kafka;

import com.incident.trace.config.KafkaTopicProperties;
import com.incident.trace.event.TraceIngestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaTraceEventProducer implements TraceEventProducer {

    private final KafkaTemplate<String, TraceIngestedEvent> kafkaTemplate;
    private final KafkaTopicProperties kafkaTopicProperties;

    @Override
    public void publishTraceIngestedEvent(TraceIngestedEvent event) {

        String topic = kafkaTopicProperties.getTraceIngested();
        String traceId = event.getPayload().getTraceId();
        kafkaTemplate.send(topic, traceId, event);
        log.info("Published trace event. traceId={} topic={}", traceId, topic);
    }
}