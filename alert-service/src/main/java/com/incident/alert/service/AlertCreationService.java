package com.incident.alert.service;

import com.incident.alert.exception.DuplicateAlertException;
import com.incident.alert.kafka.AlertEventProducer;
import com.incident.alert.mapper.AlertMapper;
import com.incident.alert.model.entity.AlertEntity;
import com.incident.alert.model.enums.AlertSeverity;
import com.incident.alert.model.kafka.TraceIngestedPayload;
import com.incident.alert.redis.AlertRedisPublisher;
import com.incident.alert.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertCreationService {
    private final AlertRepository alertRepository;
    private final AlertMapper alertMapper;
    private final AlertRedisPublisher redisPublisher;
    private final AlertEventProducer kafkaProducer;

    @Transactional
    public AlertEntity createAlertFromTrace(TraceIngestedPayload payload) {
        String traceId = payload.getTraceId();
        if (alertRepository.existsByTraceId(traceId)) {
            log.warn("Duplicate alert skipped. traceId={}", traceId);
            throw new DuplicateAlertException(traceId);
        }
        AlertSeverity severity = alertMapper.determineSeverity(payload);
        AlertEntity entity = alertMapper.toEntity(payload, severity);
        AlertEntity saved = alertRepository.save(entity);
        log.info("Alert created. alertId={} traceId={} severity={} title={}",
                saved.getAlertId(), saved.getTraceId(), saved.getSeverity(), saved.getTitle());
        redisPublisher.publishAlertCreated(saved);
        kafkaProducer.publishAlertCreated(saved);
        return saved;
    }
}
