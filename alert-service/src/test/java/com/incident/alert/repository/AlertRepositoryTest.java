package com.incident.alert.repository;

import com.incident.alert.model.entity.AlertEntity;
import com.incident.alert.model.enums.AlertSeverity;
import com.incident.alert.model.enums.AlertStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class AlertRepositoryTest {

    @Autowired
    private AlertRepository alertRepository;

    @Test
    void shouldSaveAndFindAlert() {
        AlertEntity entity = AlertEntity.builder()
                .alertId("ALT-12345")
                .traceId("trace-123")
                .severity(AlertSeverity.CRITICAL)
                .status(AlertStatus.OPEN)
                .title("Test Alert")
                .description("Test Description")
                .build();

        AlertEntity saved = alertRepository.saveAndFlush(entity);

        assertNotNull(saved.getId());
        assertNotNull(saved.getTriggeredAt());
        assertNotNull(saved.getUpdatedAt());

        Optional<AlertEntity> found = alertRepository.findByAlertId("ALT-12345");
        assertTrue(found.isPresent());
        assertEquals("trace-123", found.get().getTraceId());
    }

    @Test
    void shouldFailOnDuplicateAlertId() {
        AlertEntity entity1 = AlertEntity.builder()
                .alertId("ALT-DUP")
                .traceId("trace-1")
                .severity(AlertSeverity.HIGH)
                .title("T1")
                .description("D1")
                .build();

        AlertEntity entity2 = AlertEntity.builder()
                .alertId("ALT-DUP")
                .traceId("trace-2")
                .severity(AlertSeverity.HIGH)
                .title("T2")
                .description("D2")
                .build();

        alertRepository.saveAndFlush(entity1);
        assertThrows(DataIntegrityViolationException.class, () -> alertRepository.saveAndFlush(entity2));
    }
}
