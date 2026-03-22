package com.flightmonitor.unit.service;

import com.flightmonitor.domain.entity.PriceAlert;
import com.flightmonitor.domain.enums.AlertStatus;
import com.flightmonitor.dto.request.CreateAlertRequest;
import com.flightmonitor.dto.response.PriceAlertResponse;
import com.flightmonitor.mapper.PriceAlertMapper;
import com.flightmonitor.messaging.PriceAlertProducer;
import com.flightmonitor.repository.PriceAlertRepository;
import com.flightmonitor.service.AlertServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private PriceAlertRepository alertRepository;
    @Mock
    private PriceAlertMapper alertMapper;
    @Mock
    private PriceAlertProducer priceAlertProducer;

    private AlertServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AlertServiceImpl(alertRepository, alertMapper, priceAlertProducer);
    }

    @Test
    void createAlert_persistsWithActiveStatus() {
        CreateAlertRequest req = new CreateAlertRequest("test@test.com", "BSB", "LIS",
                LocalDate.now().plusDays(10), new BigDecimal("299.99"));
        PriceAlert saved = alertWith(AlertStatus.ACTIVE);
        when(alertRepository.save(any())).thenReturn(saved);
        PriceAlertResponse expected = mockResponse(saved);
        when(alertMapper.toResponse(saved)).thenReturn(expected);

        PriceAlertResponse result = service.createAlert(req);

        ArgumentCaptor<PriceAlert> captor = ArgumentCaptor.forClass(PriceAlert.class);
        verify(alertRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AlertStatus.ACTIVE);
    }

    @Test
    void createAlert_throwsForInvalidEmail() {
        CreateAlertRequest req = new CreateAlertRequest("not-an-email", "BSB", "LIS",
                LocalDate.now().plusDays(10), new BigDecimal("299.99"));

        assertThatThrownBy(() -> service.createAlert(req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pauseAlert_changesStatusToPaused() {
        UUID id = UUID.randomUUID();
        PriceAlert alert = alertWith(AlertStatus.ACTIVE);
        alert.setId(id);
        when(alertRepository.findById(id)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any())).thenReturn(alert);
        when(alertMapper.toResponse(any())).thenReturn(mockResponse(alert));

        service.pauseAlert(id);

        assertThat(alert.getStatus()).isEqualTo(AlertStatus.PAUSED);
    }

    @Test
    void checkAlerts_updatesLastCheckedAtForActiveAlerts() {
        PriceAlert alert = alertWith(AlertStatus.ACTIVE);
        alert.setTargetPrice(new BigDecimal("500.00"));
        when(alertRepository.findByOriginAndDestinationAndStatus(any(), any(), any()))
                .thenReturn(List.of(alert));
        when(alertRepository.save(any())).thenReturn(alert);

        service.checkAlertsForRoute("BSB", "LIS", new BigDecimal("600.00"));

        verify(alertRepository).save(alert);
        assertThat(alert.getLastCheckedAt()).isNotNull();
    }

    private PriceAlert alertWith(AlertStatus status) {
        PriceAlert a = new PriceAlert();
        a.setId(UUID.randomUUID());
        a.setUserEmail("test@test.com");
        a.setOrigin("BSB");
        a.setDestination("LIS");
        a.setDepartureDate(LocalDate.now().plusDays(10));
        a.setTargetPrice(new BigDecimal("299.99"));
        a.setStatus(status);
        return a;
    }

    private PriceAlertResponse mockResponse(PriceAlert a) {
        return new PriceAlertResponse(a.getId(), a.getUserEmail(), a.getOrigin(), a.getDestination(),
                a.getDepartureDate(), a.getTargetPrice(), a.getStatus(), null, null, null);
    }
}
