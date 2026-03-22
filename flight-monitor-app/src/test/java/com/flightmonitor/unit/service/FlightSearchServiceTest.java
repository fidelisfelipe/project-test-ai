package com.flightmonitor.unit.service;

import com.flightmonitor.client.AmadeusClient;
import com.flightmonitor.domain.enums.CabinClass;
import com.flightmonitor.dto.request.FlightSearchRequest;
import com.flightmonitor.dto.response.FlightOfferResponse;
import com.flightmonitor.mapper.FlightOfferMapper;
import com.flightmonitor.messaging.MessageBus;
import com.flightmonitor.messaging.MessageSendResult;
import com.flightmonitor.messaging.BrokerType;
import com.flightmonitor.messaging.dto.FlightSearchRequestMessage;
import com.flightmonitor.repository.FlightOfferRepository;
import com.flightmonitor.repository.SearchLogRepository;
import com.flightmonitor.service.AlertService;
import com.flightmonitor.service.FlightSearchServiceImpl;
import com.flightmonitor.service.PriceHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlightSearchServiceTest {

    @Mock
    private AmadeusClient amadeusClient;
    @Mock
    private FlightOfferRepository flightOfferRepository;
    @Mock
    private SearchLogRepository searchLogRepository;
    @Mock
    private PriceHistoryService priceHistoryService;
    @Mock
    private AlertService alertService;
    @Mock
    private MessageBus messageBus;
    @Mock
    private FlightOfferMapper flightOfferMapper;

    private FlightSearchServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(messageBus.sendSearchRequest(any())).thenReturn(
                CompletableFuture.completedFuture(
                        MessageSendResult.ok(BrokerType.SYNC, "test-id", 0)));
        service = new FlightSearchServiceImpl(
                amadeusClient, flightOfferRepository, searchLogRepository,
                priceHistoryService, alertService, messageBus, flightOfferMapper);
    }

    @Test
    void searchFlights_returnsSortedByPrice() {
        FlightSearchRequest request = validRequest();
        FlightOfferResponse offer1 = buildOffer(new BigDecimal("500.00"));
        FlightOfferResponse offer2 = buildOffer(new BigDecimal("299.99"));
        when(amadeusClient.searchFlights(any())).thenReturn(List.of(offer1, offer2));
        when(flightOfferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(searchLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(priceHistoryService).saveSnapshot(anyString(), anyString(), any(), anyList());
        doNothing().when(alertService).checkAlertsForRoute(anyString(), anyString(), any());

        List<FlightOfferResponse> results = service.searchFlights(request);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).price()).isEqualByComparingTo("299.99");
        assertThat(results.get(1).price()).isEqualByComparingTo("500.00");
    }

    @Test
    void searchFlights_throwsForPastDate() {
        FlightSearchRequest request = new FlightSearchRequest(
                "BSB", "LIS", LocalDate.now().minusDays(1), null, 1, CabinClass.ECONOMY);

        assertThatThrownBy(() -> service.searchFlights(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("future");
    }

    @Test
    void searchFlights_throwsForInvalidIata() {
        FlightSearchRequest request = new FlightSearchRequest(
                "B", "LIS", LocalDate.now().plusDays(5), null, 1, CabinClass.ECONOMY);

        assertThatThrownBy(() -> service.searchFlights(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("IATA");
    }

    @Test
    void searchFlights_publishesMessageViaMessageBus() {
        FlightSearchRequest request = validRequest();
        when(amadeusClient.searchFlights(any())).thenReturn(List.of());
        when(searchLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(priceHistoryService).saveSnapshot(anyString(), anyString(), any(), anyList());

        service.searchFlights(request);

        ArgumentCaptor<FlightSearchRequestMessage> captor = ArgumentCaptor.forClass(FlightSearchRequestMessage.class);
        verify(messageBus).sendSearchRequest(captor.capture());
        assertThat(captor.getValue().origin()).isEqualTo("BSB");
        assertThat(captor.getValue().destination()).isEqualTo("LIS");
    }

    @Test
    void searchFlights_recordsSearchLogWithDurationMs() {
        FlightSearchRequest request = validRequest();
        when(amadeusClient.searchFlights(any())).thenReturn(List.of());
        doNothing().when(priceHistoryService).saveSnapshot(anyString(), anyString(), any(), anyList());
        ArgumentCaptor<com.flightmonitor.domain.entity.SearchLog> logCaptor =
                ArgumentCaptor.forClass(com.flightmonitor.domain.entity.SearchLog.class);
        when(searchLogRepository.save(logCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        service.searchFlights(request);

        assertThat(logCaptor.getValue().getDurationMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void checkAlerts_firesPriceAlertWhenPriceMeetsTarget() {
        when(amadeusClient.searchFlights(any())).thenReturn(List.of(buildOffer(new BigDecimal("150.00"))));
        when(flightOfferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(searchLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(priceHistoryService).saveSnapshot(anyString(), anyString(), any(), anyList());
        doNothing().when(alertService).checkAlertsForRoute(eq("BSB"), eq("LIS"), eq(new BigDecimal("150.00")));

        service.searchFlights(validRequest());

        verify(alertService).checkAlertsForRoute("BSB", "LIS", new BigDecimal("150.00"));
    }

    @Test
    void checkAlerts_doesNotCallAlertServiceWhenNoResults() {
        when(amadeusClient.searchFlights(any())).thenReturn(List.of());
        when(searchLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(priceHistoryService).saveSnapshot(anyString(), anyString(), any(), anyList());

        service.searchFlights(validRequest());

        verify(alertService, never()).checkAlertsForRoute(anyString(), anyString(), any());
    }

    private FlightSearchRequest validRequest() {
        return new FlightSearchRequest("BSB", "LIS", LocalDate.now().plusDays(5), null, 1, CabinClass.ECONOMY);
    }

    private FlightOfferResponse buildOffer(BigDecimal price) {
        return new FlightOfferResponse(UUID.randomUUID(), "BSB", "LIS",
                LocalDate.now().plusDays(5), null, "TP", price, "EUR", 0, "PT5H", CabinClass.ECONOMY, null, LocalDateTime.now(), "AMADEUS");
    }
}
