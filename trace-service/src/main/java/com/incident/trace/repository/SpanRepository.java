package com.incident.trace.repository;

import com.incident.trace.entity.SpanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import com.incident.trace.enums.SpanStatus;

import java.util.List;
import java.util.UUID;

public interface SpanRepository
        extends JpaRepository<SpanEntity, UUID> {

    List<SpanEntity> findByTrace_TraceId(
            String traceId
    );

    List<SpanEntity> findByStatus(
            SpanStatus status
    );

    List<SpanEntity> findBySpanId(
            String spanId
    );

    long countByTrace_TraceId(String traceId);
}