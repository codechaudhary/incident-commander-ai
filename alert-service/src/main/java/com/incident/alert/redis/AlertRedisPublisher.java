package com.incident.alert.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incident.alert.model.entity.AlertEntity;
import com.incident.alert.mapper.AlertMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertRedisPublisher {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AlertMapper alertMapper;

    @Value("${alert.redis.channels.alerts-live}")
    private String alertsLiveChannel;

    @Async
    public void publishAlertCreated(AlertEntity entity) {
        try {
            Map<String, Object> message = alertMapper.toRedisMessage(entity);
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(alertsLiveChannel, json);
            log.info("Published to Redis. channel={} alertId={} traceId={}", alertsLiveChannel, entity.getAlertId(), entity.getTraceId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Redis message. alertId={} error={}", entity.getAlertId(), e.getMessage());
        } catch (Exception e) {
            log.error("Failed to publish to Redis. alertId={} error={}", entity.getAlertId(), e.getMessage(), e);
        }
    }
}
