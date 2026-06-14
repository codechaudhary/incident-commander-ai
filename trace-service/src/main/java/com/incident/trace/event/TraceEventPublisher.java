package com.incident.trace.event;

import com.incident.trace.entity.TraceEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TraceEventPublisher {

    private final ApplicationEventPublisher
            applicationEventPublisher;

    public void publishTracePersistedEvent(
            TraceEntity traceEntity,
            Integer spanCount
    ) {

        applicationEventPublisher.publishEvent(
                new TracePersistedApplicationEvent(
                        traceEntity,
                        spanCount
                )
        );
    }
}