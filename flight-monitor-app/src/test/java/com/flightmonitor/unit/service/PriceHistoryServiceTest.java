package com.flightmonitor.unit.service;

import com.flightmonitor.domain.entity.PriceHistory;
import com.flightmonitor.dto.response.FlightOfferResponse;
import com.flightmonitor.dto.response.PriceHistoryResponse;
import com.flightmonitor.domain.enums.CabinClass;
import com.flightmonitor.repository.PriceHistoryRepository;
import com.flightmonitor.service.PriceHistoryServiceImpl;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceHistoryServiceTest {

    @Mock
    private PriceHistoryRepository repository;

    private PriceHistoryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PriceHistoryServiceImpl(repository);
    }

    @Test
    void getHistory_returnsSortedByRecordedAtDesc() {
        PriceHistory h1 = buildHistory(LocalDateTime.now().minusDays(1), new BigDecimal("300"));
        PriceHistory h2 = buildHistory(LocalDateTime.now(), new BigDecimal("250"));
        when(repository.findByOriginAndDestinationAndRecordedAtAfterOrderByRecordedAtDesc(anyString(), anyString(), any()))
                .thenReturn(List.of(h2, h1));

        List<PriceHistoryResponse> result = service.getHistory("BSB", "LIS", 30);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).avgPrice()).isEqualByComparingTo("250");
    }

    @Test
    void calculateTrend_returnsUp() {
        PriceHistoryResponse r1 = historyResponse(new BigDecimal("350"));
        PriceHistoryResponse r2 = historyResponse(new BigDecimal("300"));

        String trend = service.calculateTrend(List.of(r1, r2));

        assertThat(trend).isEqualTo("UP");
    }

    @Test
    void calculateTrend_returnsDown() {
        PriceHistoryResponse r1 = historyResponse(new BigDecimal("250"));
        PriceHistoryResponse r2 = historyResponse(new BigDecimal("300"));

        String trend = service.calculateTrend(List.of(r1, r2));

        assertThat(trend).isEqualTo("DOWN");
    }

    @Test
    void calculateTrend_returnsStable() {
        PriceHistoryResponse r1 = historyResponse(new BigDecimal("300"));
        PriceHistoryResponse r2 = historyResponse(new BigDecimal("300"));

        String trend = service.calculateTrend(List.of(r1, r2));

        assertThat(trend).isEqualTo("STABLE");
    }

    @Test
    void saveSnapshot_createsHistoryWithCorrectMinMaxAvg() {
        FlightOfferResponse o1 = buildOffer(new BigDecimal("100"));
        FlightOfferResponse o2 = buildOffer(new BigDecimal("200"));
        FlightOfferResponse o3 = buildOffer(new BigDecimal("300"));
        when(repository.findByOriginAndDestinationAndDepartureDateOrderByRecordedAtDesc(anyString(), anyString(), any()))
                .thenReturn(List.of());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.saveSnapshot("BSB", "LIS", LocalDate.now().plusDays(5), List.of(o1, o2, o3));

        ArgumentCaptor<PriceHistory> captor = ArgumentCaptor.forClass(PriceHistory.class);
        verify(repository).save(captor.capture());
        PriceHistory saved = captor.getValue();
        assertThat(saved.getMinPrice()).isEqualByComparingTo("100");
        assertThat(saved.getMaxPrice()).isEqualByComparingTo("300");
        assertThat(saved.getAvgPrice()).isEqualByComparingTo("200.00");
    }

    private PriceHistory buildHistory(LocalDateTime recordedAt, BigDecimal avg) {
        PriceHistory h = new PriceHistory();
        h.setId(UUID.randomUUID());
        h.setOrigin("BSB");
        h.setDestination("LIS");
        h.setDepartureDate(LocalDate.now().plusDays(5));
        h.setMinPrice(avg.subtract(BigDecimal.TEN));
        h.setMaxPrice(avg.add(BigDecimal.TEN));
        h.setAvgPrice(avg);
        h.setOffersCount(5);
        h.setRecordedAt(recordedAt);
        h.setTrend("STABLE");
        return h;
    }

    private PriceHistoryResponse historyResponse(BigDecimal avg) {
        return new PriceHistoryResponse(UUID.randomUUID(), "BSB", "LIS",
                LocalDate.now().plusDays(5), avg.subtract(BigDecimal.TEN), avg.add(BigDecimal.TEN),
                avg, 5, LocalDateTime.now(), "STABLE");
    }

    private FlightOfferResponse buildOffer(BigDecimal price) {
        return new FlightOfferResponse(UUID.randomUUID(), "BSB", "LIS",
                LocalDate.now().plusDays(5), null, "TP", price, "EUR", 0, "PT5H", CabinClass.ECONOMY, null, LocalDateTime.now(), "AMADEUS");
    }
}
