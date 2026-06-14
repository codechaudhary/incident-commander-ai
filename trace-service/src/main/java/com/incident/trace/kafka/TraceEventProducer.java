package com.incident.trace.kafka;

import com.incident.trace.event.TraceIngestedEvent;

public interface TraceEventProducer {

    void publishTraceIngestedEvent(
            TraceIngestedEvent event
    );
}