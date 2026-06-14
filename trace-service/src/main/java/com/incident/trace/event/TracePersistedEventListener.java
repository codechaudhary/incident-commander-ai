package com.incident.trace.event;

import com.incident.trace.entity.TraceEntity;
import com.incident.trace.kafka.TraceEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TracePersistedEventListener {

    private final TraceEventProducer
            traceEventProducer;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleTracePersisted(
            TracePersistedApplicationEvent event
    ) {

        TraceEntity trace =
                event.getTraceEntity();

        TraceIngestedPayload payload =
                TraceIngestedPayload.builder()
                        .traceId(trace.getTraceId())
                        .rootService(trace.getRootService())
                        .rootOperation(trace.getRootOperation())
                        .status(trace.getStatus().name())
                        .failureType(trace.getFailureType().name())
                        .durationMs(trace.getDurationMs())
                        .startedAt(trace.getStartedAt())
                        .endedAt(trace.getEndedAt())
                        .spanCount(event.getSpanCount())
                        .errorSpans(List.of())
                        .build();

        TraceIngestedEvent kafkaEvent =
                TraceIngestedEvent.builder()
                        .eventId(
                                UUID.randomUUID().toString()
                        )
                        .eventType(
                                "TRACE_INGESTED"
                        )
                        .eventVersion(
                                "1.0"
                        )
                        .timestamp(
                                Instant.now()
                        )
                        .source(
                                "trace-service"
                        )
                        .payload(payload)
                        .build();

        traceEventProducer
                .publishTraceIngestedEvent(
                        kafkaEvent
                );

        log.info(
                "Trace event published after successful commit. traceId={} spanCount={}",
                trace.getTraceId(),
                event.getSpanCount()
        );
    }
}