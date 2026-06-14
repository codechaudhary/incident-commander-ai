package com.incident.alert.kafka;

import com.incident.alert.exception.DuplicateAlertException;
import com.incident.alert.model.kafka.TraceIngestedEvent;
import com.incident.alert.model.kafka.TraceIngestedPayload;
import com.incident.alert.service.AlertCreationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class TraceEventConsumer {
    private static final Set<String> ALERTABLE_STATUSES = Set.of("ERROR", "TIMEOUT");
    private final AlertCreationService alertCreationService;

    @KafkaListener(
            topics = "${alert.kafka.topics.trace-ingested}",
            groupId = "${spring.kafka.consumer.group-id}",
            concurrency = "3"
    )
    public void onTraceIngested(ConsumerRecord<String, TraceIngestedEvent> record) {
        TraceIngestedEvent event = record.value();
        if (event == null || event.getPayload() == null) {
            log.warn("Received null or empty trace event. offset={} partition={}", record.offset(), record.partition());
            return;
        }
        TraceIngestedPayload payload = event.getPayload();
        String traceId = payload.getTraceId();
        String status = payload.getStatus();
        if (!ALERTABLE_STATUSES.contains(status)) {
            log.debug("Skipping non-alertable trace. traceId={} status={}", traceId, status);
            return;
        }
        log.info("Processing trace event. traceId={} status={} failureType={} eventId={}",
                traceId, status, payload.getFailureType(), event.getEventId());
        try {
            alertCreationService.createAlertFromTrace(payload);
        } catch (DuplicateAlertException e) {
            log.debug("Duplicate alert ignored. traceId={}", traceId);
        } catch (Exception e) {
            log.error("Failed to create alert from trace event. traceId={} error={}", traceId, e.getMessage(), e);
        }
    }
}
