package com.incident.trace.repository;

import com.incident.trace.entity.TraceEntity;
import com.incident.trace.enums.TraceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TraceRepository
        extends JpaRepository<TraceEntity, UUID> {

    Optional<TraceEntity> findByTraceId(String traceId);

    Page<TraceEntity> findByStatus(
            TraceStatus status,
            Pageable pageable
    );

    Page<TraceEntity> findByStartedAtBetween(
            Instant from,
            Instant to,
            Pageable pageable
    );

    Page<TraceEntity> findByStatusAndStartedAtBetween(
            TraceStatus status,
            Instant from,
            Instant to,
            Pageable pageable
    );
}