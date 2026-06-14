package com.incident.alert.service;

import com.incident.alert.exception.DuplicateAlertException;
import com.incident.alert.kafka.AlertEventProducer;
import com.incident.alert.mapper.AlertMapper;
import com.incident.alert.model.entity.AlertEntity;
import com.incident.alert.model.enums.AlertSeverity;
import com.incident.alert.model.kafka.TraceIngestedPayload;
import com.incident.alert.redis.AlertRedisPublisher;
import com.incident.alert.repository.AlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertCreationServiceTest {

    @Mock
    private AlertRepository alertRepository;
    @Mock
    private AlertMapper alertMapper;
    @Mock
    private AlertRedisPublisher redisPublisher;
    @Mock
    private AlertEventProducer kafkaProducer;

    @InjectMocks
    private AlertCreationService alertCreationService;

    @Test
    void shouldCreateAlertFromTrace() {
        TraceIngestedPayload payload = TraceIngestedPayload.builder()
                .traceId("trace-1")
                .failureType("RUNTIME_EXCEPTION")
                .build();
        
        AlertEntity entityToSave = AlertEntity.builder().alertId("ALT-1").traceId("trace-1").build();
        AlertEntity savedEntity = AlertEntity.builder()
                .id(UUID.randomUUID())
                .alertId("ALT-1")
                .traceId("trace-1")
                .severity(AlertSeverity.CRITICAL)
                .title("Test")
                .triggeredAt(Instant.now())
                .build();

        when(alertRepository.existsByTraceId("trace-1")).thenReturn(false);
        when(alertMapper.determineSeverity(payload)).thenReturn(AlertSeverity.CRITICAL);
        when(alertMapper.toEntity(payload, AlertSeverity.CRITICAL)).thenReturn(entityToSave);
        when(alertRepository.save(entityToSave)).thenReturn(savedEntity);

        AlertEntity result = alertCreationService.createAlertFromTrace(payload);

        assertEquals("ALT-1", result.getAlertId());
        verify(redisPublisher, times(1)).publishAlertCreated(savedEntity);
        verify(kafkaProducer, times(1)).publishAlertCreated(savedEntity);
    }

    @Test
    void shouldThrowExceptionWhenDuplicateTraceId() {
        TraceIngestedPayload payload = TraceIngestedPayload.builder()
                .traceId("trace-dup")
                .build();

        when(alertRepository.existsByTraceId("trace-dup")).thenReturn(true);

        assertThrows(DuplicateAlertException.class, () -> alertCreationService.createAlertFromTrace(payload));
        verify(alertRepository, never()).save(any());
    }
}
