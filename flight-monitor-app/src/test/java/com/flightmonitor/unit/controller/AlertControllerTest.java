package com.flightmonitor.unit.controller;

import com.flightmonitor.controller.AlertController;
import com.flightmonitor.domain.enums.AlertStatus;
import com.flightmonitor.dto.response.PriceAlertResponse;
import com.flightmonitor.exception.GlobalExceptionHandler;
import com.flightmonitor.service.AlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AlertControllerTest {

    @Mock
    private AlertService alertService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AlertController controller = new AlertController(alertService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getAlerts_returns200WithAlertList() throws Exception {
        UUID id = UUID.randomUUID();
        PriceAlertResponse response = new PriceAlertResponse(id, "test@test.com", "BSB", "LIS",
                LocalDate.now().plusDays(10), new BigDecimal("299.99"), AlertStatus.ACTIVE, null, LocalDateTime.now(), null);
        when(alertService.getAlertsByEmail(anyString())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/alerts").param("email", "test@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userEmail").value("test@test.com"));
    }
}
