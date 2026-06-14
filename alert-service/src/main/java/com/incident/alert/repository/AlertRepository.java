package com.incident.alert.repository;

import com.incident.alert.model.entity.AlertEntity;
import com.incident.alert.model.enums.AlertSeverity;
import com.incident.alert.model.enums.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<AlertEntity, UUID> {
    Optional<AlertEntity> findByAlertId(String alertId);
    Optional<AlertEntity> findFirstByTraceId(String traceId);
    boolean existsByTraceId(String traceId);
    Page<AlertEntity> findByStatus(AlertStatus status, Pageable pageable);
    Page<AlertEntity> findBySeverity(AlertSeverity severity, Pageable pageable);
    Page<AlertEntity> findByTraceId(String traceId, Pageable pageable);
    Page<AlertEntity> findByStatusAndSeverity(AlertStatus status, AlertSeverity severity, Pageable pageable);
    Page<AlertEntity> findByStatusAndTraceId(AlertStatus status, String traceId, Pageable pageable);
    Page<AlertEntity> findBySeverityAndTraceId(AlertSeverity severity, String traceId, Pageable pageable);
    Page<AlertEntity> findByStatusAndSeverityAndTraceId(AlertStatus status, AlertSeverity severity, String traceId, Pageable pageable);
    long countByStatus(AlertStatus status);
    long countByStatusAndSeverity(AlertStatus status, AlertSeverity severity);
}
