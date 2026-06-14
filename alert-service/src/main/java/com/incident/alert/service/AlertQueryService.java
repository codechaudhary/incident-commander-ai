package com.incident.alert.service;

import com.incident.alert.exception.AlertNotFoundException;
import com.incident.alert.exception.InvalidAlertStatusException;
import com.incident.alert.mapper.AlertMapper;
import com.incident.alert.model.dto.AlertResponse;
import com.incident.alert.model.dto.PagedResponse;
import com.incident.alert.model.entity.AlertEntity;
import com.incident.alert.model.enums.AlertSeverity;
import com.incident.alert.model.enums.AlertStatus;
import com.incident.alert.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlertQueryService {
    private final AlertRepository alertRepository;
    private final AlertMapper alertMapper;

    public AlertResponse getAlertByAlertId(String alertId) {
        log.debug("Fetching alert. alertId={}", alertId);
        AlertEntity entity = alertRepository.findByAlertId(alertId)
                .orElseThrow(() -> new AlertNotFoundException(alertId));
        return alertMapper.toResponse(entity);
    }

    public PagedResponse<AlertResponse> getAlerts(AlertStatus status, AlertSeverity severity, String traceId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "triggeredAt"));
        Page<AlertEntity> result = executeQuery(status, severity, traceId, pageRequest);
        log.debug("Queried alerts. page={} size={} total={} filters=[status={}, severity={}, traceId={}]",
                page, size, result.getTotalElements(), status, severity, traceId);
        return alertMapper.toPagedResponse(result, result.getContent().stream().map(alertMapper::toResponse).toList());
    }

    @Transactional
    public AlertResponse updateAlertStatus(String alertId, AlertStatus newStatus) {
        if (newStatus == AlertStatus.OPEN) {
            throw new InvalidAlertStatusException("Cannot set status back to OPEN");
        }
        AlertEntity entity = alertRepository.findByAlertId(alertId)
                .orElseThrow(() -> new AlertNotFoundException(alertId));
        if (entity.getStatus() == AlertStatus.RESOLVED) {
            throw new InvalidAlertStatusException("Cannot update a RESOLVED alert");
        }
        if (entity.getStatus() == newStatus) {
            log.debug("Alert status unchanged. alertId={} status={}", alertId, newStatus);
            return alertMapper.toResponse(entity);
        }
        AlertStatus oldStatus = entity.getStatus();
        entity.setStatus(newStatus);
        AlertEntity saved = alertRepository.save(entity);
        log.info("Alert status updated. alertId={} oldStatus={} newStatus={}", alertId, oldStatus, newStatus);
        return alertMapper.toResponse(saved);
    }

    private Page<AlertEntity> executeQuery(AlertStatus status, AlertSeverity severity, String traceId, PageRequest pr) {
        boolean hasStatus = status != null;
        boolean hasSeverity = severity != null;
        boolean hasTrace = traceId != null && !traceId.isBlank();
        if (hasStatus && hasSeverity && hasTrace) return alertRepository.findByStatusAndSeverityAndTraceId(status, severity, traceId, pr);
        if (hasStatus && hasSeverity) return alertRepository.findByStatusAndSeverity(status, severity, pr);
        if (hasStatus && hasTrace) return alertRepository.findByStatusAndTraceId(status, traceId, pr);
        if (hasSeverity && hasTrace) return alertRepository.findBySeverityAndTraceId(severity, traceId, pr);
        if (hasStatus) return alertRepository.findByStatus(status, pr);
        if (hasSeverity) return alertRepository.findBySeverity(severity, pr);
        if (hasTrace) return alertRepository.findByTraceId(traceId, pr);
        return alertRepository.findAll(pr);
    }
}
