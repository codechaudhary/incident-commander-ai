package com.incident.alert.model.entity;

import com.incident.alert.model.enums.AlertSeverity;
import com.incident.alert.model.enums.AlertStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alerts", indexes = {
        @Index(name = "idx_alerts_alert_id", columnList = "alert_id"),
        @Index(name = "idx_alerts_trace_id", columnList = "trace_id"),
        @Index(name = "idx_alerts_status", columnList = "status"),
        @Index(name = "idx_alerts_severity", columnList = "severity"),
        @Index(name = "idx_alerts_triggered_at", columnList = "triggered_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "alert_id", nullable = false, unique = true, length = 64)
    private String alertId;

    @Column(name = "trace_id", nullable = false, length = 64)
    private String traceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private AlertStatus status;

    @Column(nullable = false, length = 256)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "triggered_at", nullable = false, updatable = false)
    private Instant triggeredAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (triggeredAt == null) triggeredAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = AlertStatus.OPEN;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
