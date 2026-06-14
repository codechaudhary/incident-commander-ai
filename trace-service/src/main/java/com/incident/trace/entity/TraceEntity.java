package com.incident.trace.entity;

import com.incident.trace.enums.FailureType;
import com.incident.trace.enums.TraceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "traces",
        indexes = {
                @Index(name = "idx_traces_trace_id", columnList = "trace_id"),
                @Index(name = "idx_traces_status", columnList = "status"),
                @Index(name = "idx_traces_started_at", columnList = "started_at"),
                @Index(name="idx_traces_root_service", columnList = "root_service"),
                @Index(name="idx_traces_root_operation", columnList = "root_operation")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TraceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
            name = "trace_id",
            nullable = false,
            unique = true,
            length = 32
    )
    private String traceId;

    @Column(
            name = "root_service",
            nullable = false,
            length = 128
    )
    private String rootService;

    @Column(
            name = "root_operation",
            nullable = false,
            length = 128
    )
    private String rootOperation;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 16
    )
    private TraceStatus status;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "failure_type",
            nullable = false,
            length = 32
    )
    private FailureType failureType;

    @Column(
            name = "duration_ms",
            nullable = false
    )
    private Long durationMs;

    @Column(
            name = "started_at",
            nullable = false
    )
    private Instant startedAt;

    @Column(
            name = "ended_at",
            nullable = false
    )
    private Instant endedAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @OneToMany(
            mappedBy = "trace",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<SpanEntity> spans = new ArrayList<>();

    @PrePersist
    public void prePersist() {

        if (createdAt == null) {
            createdAt = Instant.now();
        }

        if (failureType == null) {
            failureType = FailureType.NONE;
        }
    }
}