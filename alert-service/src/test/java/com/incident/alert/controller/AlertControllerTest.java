package com.incident.alert.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incident.alert.exception.AlertNotFoundException;
import com.incident.alert.exception.InvalidAlertStatusException;
import com.incident.alert.model.dto.AlertResponse;
import com.incident.alert.model.dto.AlertStatusUpdateRequest;
import com.incident.alert.model.dto.PagedResponse;
import com.incident.alert.model.enums.AlertStatus;
import com.incident.alert.service.AlertQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AlertController.class)
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AlertQueryService alertQueryService;

    @Test
    void shouldGetAlerts() throws Exception {
        PagedResponse<AlertResponse> response = PagedResponse.<AlertResponse>builder()
                .content(List.of(AlertResponse.builder().alertId("ALT-1").build()))
                .totalElements(1L)
                .build();

        when(alertQueryService.getAlerts(any(), any(), any(), eq(0), eq(20))).thenReturn(response);

        mockMvc.perform(get("/api/v1/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].alertId").value("ALT-1"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldUpdateStatus() throws Exception {
        AlertStatusUpdateRequest req = new AlertStatusUpdateRequest(AlertStatus.ACKNOWLEDGED);
        AlertResponse resp = AlertResponse.builder().alertId("ALT-1").status(AlertStatus.ACKNOWLEDGED).build();

        when(alertQueryService.updateAlertStatus("ALT-1", AlertStatus.ACKNOWLEDGED)).thenReturn(resp);

        mockMvc.perform(patch("/api/v1/alerts/ALT-1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));
    }

    @Test
    void shouldHandleNotFoundException() throws Exception {
        AlertStatusUpdateRequest req = new AlertStatusUpdateRequest(AlertStatus.ACKNOWLEDGED);

        when(alertQueryService.updateAlertStatus("ALT-MISSING", AlertStatus.ACKNOWLEDGED))
                .thenThrow(new AlertNotFoundException("ALT-MISSING"));

        mockMvc.perform(patch("/api/v1/alerts/ALT-MISSING/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void shouldHandleInvalidStatusException() throws Exception {
        AlertStatusUpdateRequest req = new AlertStatusUpdateRequest(AlertStatus.OPEN);

        when(alertQueryService.updateAlertStatus("ALT-1", AlertStatus.OPEN))
                .thenThrow(new InvalidAlertStatusException("Cannot revert to OPEN"));

        mockMvc.perform(patch("/api/v1/alerts/ALT-1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Cannot revert to OPEN"));
    }
}
