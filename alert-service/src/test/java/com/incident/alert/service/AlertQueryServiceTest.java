package com.incident.alert.service;

import com.incident.alert.exception.AlertNotFoundException;
import com.incident.alert.exception.InvalidAlertStatusException;
import com.incident.alert.mapper.AlertMapper;
import com.incident.alert.model.dto.AlertResponse;
import com.incident.alert.model.entity.AlertEntity;
import com.incident.alert.model.enums.AlertStatus;
import com.incident.alert.repository.AlertRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertQueryServiceTest {

    @Mock
    private AlertRepository alertRepository;
    @Mock
    private AlertMapper alertMapper;

    @InjectMocks
    private AlertQueryService alertQueryService;

    @Test
    void shouldUpdateAlertStatus() {
        AlertEntity entity = new AlertEntity();
        entity.setStatus(AlertStatus.OPEN);
        entity.setAlertId("ALT-1");

        when(alertRepository.findByAlertId("ALT-1")).thenReturn(Optional.of(entity));
        when(alertRepository.save(any(AlertEntity.class))).thenAnswer(i -> i.getArguments()[0]);
        when(alertMapper.toResponse(any())).thenReturn(AlertResponse.builder().status(AlertStatus.ACKNOWLEDGED).build());

        AlertResponse response = alertQueryService.updateAlertStatus("ALT-1", AlertStatus.ACKNOWLEDGED);

        assertEquals(AlertStatus.ACKNOWLEDGED, response.getStatus());
        assertEquals(AlertStatus.ACKNOWLEDGED, entity.getStatus());
        verify(alertRepository, times(1)).save(entity);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingToOpen() {
        assertThrows(InvalidAlertStatusException.class, 
                () -> alertQueryService.updateAlertStatus("ALT-1", AlertStatus.OPEN));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingResolvedAlert() {
        AlertEntity entity = new AlertEntity();
        entity.setStatus(AlertStatus.RESOLVED);

        when(alertRepository.findByAlertId("ALT-1")).thenReturn(Optional.of(entity));

        assertThrows(InvalidAlertStatusException.class, 
                () -> alertQueryService.updateAlertStatus("ALT-1", AlertStatus.ACKNOWLEDGED));
    }

    @Test
    void shouldThrowNotFoundException() {
        when(alertRepository.findByAlertId("ALT-MISSING")).thenReturn(Optional.empty());

        assertThrows(AlertNotFoundException.class, 
                () -> alertQueryService.updateAlertStatus("ALT-MISSING", AlertStatus.ACKNOWLEDGED));
    }
}
