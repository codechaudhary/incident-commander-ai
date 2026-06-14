package com.incident.trace.entity;

import com.incident.trace.enums.SpanStatus;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "spans",
        indexes = {
                @Index(name = "idx_spans_trace_id", columnList = "trace_id"),
                @Index(name = "idx_spans_span_id", columnList = "span_id"),
                @Index(name="idx_service_name", columnList = "service_name"),
                @Index(name="idx_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "trace_id",
            referencedColumnName = "trace_id",
            nullable = false
    )
    private TraceEntity trace;

    @Column(
            name = "span_id",
            nullable = false,
            length = 32
    )
    private String spanId;

    @Column(
            name = "parent_span_id",
            length = 32
    )
    private String parentSpanId;

    @Column(
            name = "service_name",
            nullable = false,
            length = 128
    )
    private String serviceName;

    @Column(
            name = "operation",
            nullable = false,
            length = 256
    )
    private String operation;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 16
    )
    private SpanStatus status;

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

    /**
     * OTel attributes.
     *
     * Example:
     * {
     *   "http.method":"POST",
     *   "http.status_code":500,
     *   "failure.type":"DB_TIMEOUT"
     * }
     */
    @Type(JsonType.class)
    @Column(
            name = "attributes",
            columnDefinition = "jsonb",
            nullable = false
    )
    private Map<String, Object> attributes;

    /**
     * OTel events.
     */
    @Type(JsonType.class)
    @Column(
            name = "events",
            columnDefinition = "jsonb",
            nullable = false
    )
    private List<Map<String, Object>> events;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @PrePersist
    public void prePersist() {

        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}