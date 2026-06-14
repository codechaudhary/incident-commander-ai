package com.incident.trace.event;

import com.incident.trace.entity.TraceEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TracePersistedApplicationEvent {

    private final TraceEntity traceEntity;

    private final Integer spanCount;
}