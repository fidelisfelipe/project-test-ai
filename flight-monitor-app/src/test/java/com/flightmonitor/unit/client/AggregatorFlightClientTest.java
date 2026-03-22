package com.flightmonitor.unit.client;

import com.flightmonitor.client.AggregatorFlightClient;
import com.flightmonitor.client.AmadeusClientImpl;
import com.flightmonitor.client.AmadeusClientMock;
import com.flightmonitor.client.KiwiTequilaClient;
import com.flightmonitor.client.SerpApiFlightClient;
import com.flightmonitor.config.AggregatorConfig;
import com.flightmonitor.domain.enums.CabinClass;
import com.flightmonitor.dto.request.FlightSearchRequest;
import com.flightmonitor.dto.response.FlightOfferResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AggregatorFlightClientTest {

    @Mock
    private SerpApiFlightClient serpApiClient;
    @Mock
    private KiwiTequilaClient kiwiClient;
    @Mock
    private AmadeusClientImpl amadeusClientImpl;
    @Mock
    private AmadeusClientMock mockClient;
    @Mock
    private ObjectProvider<AmadeusClientImpl> amadeusProvider;
    @Mock
    private ObjectProvider<SerpApiFlightClient> serpApiProvider;
    @Mock
    private ObjectProvider<KiwiTequilaClient> kiwiProvider;
    @Mock
    private ObjectProvider<AmadeusClientMock> mockClientProvider;

    private AggregatorFlightClient aggregator;
    private AggregatorConfig config;

    @BeforeEach
    void setUp() {
        config = new AggregatorConfig();
        config.setEnabled(true);
        config.setSources(List.of("serpapi", "kiwi"));
        config.setTimeoutSeconds(5);
        config.setDeduplicateByFlightNumber(true);

        // Register serpapi and kiwi clients via ObjectProvider
        org.mockito.Mockito.doAnswer(inv -> {
            java.util.function.Consumer<SerpApiFlightClient> consumer = inv.getArgument(0);
            consumer.accept(serpApiClient);
            return null;
        }).when(serpApiProvider).ifAvailable(any());

        org.mockito.Mockito.doAnswer(inv -> {
            java.util.function.Consumer<KiwiTequilaClient> consumer = inv.getArgument(0);
            consumer.accept(kiwiClient);
            return null;
        }).when(kiwiProvider).ifAvailable(any());

        aggregator = new AggregatorFlightClient(config, amadeusProvider, serpApiProvider, kiwiProvider, mockClientProvider);
    }

    @Test
    void searchFlights_aggregatesResultsFromTwoSources() {
        FlightSearchRequest request = validRequest();
        when(serpApiClient.searchFlights(any())).thenReturn(List.of(
                buildOffer("TP001", 3000, "SERPAPI_GOOGLE_FLIGHTS"),
                buildOffer("TP002", 3500, "SERPAPI_GOOGLE_FLIGHTS")
        ));
        when(kiwiClient.searchFlights(any())).thenReturn(List.of(
                buildOffer("IB001", 2800, "KIWI_TEQUILA"),
                buildOffer("IB002", 4000, "KIWI_TEQUILA")
        ));

        List<FlightOfferResponse> results = aggregator.searchFlights(request);

        assertThat(results).hasSize(4);
        assertThat(results.get(0).price()).isEqualByComparingTo("2800");
        assertThat(results.get(1).price()).isEqualByComparingTo("3000");
        assertThat(results.get(2).price()).isEqualByComparingTo("3500");
        assertThat(results.get(3).price()).isEqualByComparingTo("4000");
    }

    @Test
    void searchFlights_deduplicatesKeepingLowerPrice() {
        FlightSearchRequest request = validRequest();
        // Same airline + same date + same route = duplicate key
        when(serpApiClient.searchFlights(any())).thenReturn(List.of(
                buildOffer("TP083", 3200, "SERPAPI_GOOGLE_FLIGHTS")
        ));
        when(kiwiClient.searchFlights(any())).thenReturn(List.of(
                buildOffer("TP083", 2900, "KIWI_TEQUILA")
        ));

        List<FlightOfferResponse> results = aggregator.searchFlights(request);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).price()).isEqualByComparingTo("2900");
    }

    @Test
    void searchFlights_failureOfOneSourceDoesNotCancelOther() {
        FlightSearchRequest request = validRequest();
        when(serpApiClient.searchFlights(any())).thenThrow(new RuntimeException("timeout"));
        when(kiwiClient.searchFlights(any())).thenReturn(List.of(
                buildOffer("IB001", 2800, "KIWI_TEQUILA"),
                buildOffer("IB002", 3100, "KIWI_TEQUILA")
        ));

        List<FlightOfferResponse> results = aggregator.searchFlights(request);

        assertThat(results).hasSize(2);
    }

    @Test
    void searchFlights_returnsEmptyWhenNoConfiguredSourceAvailable() {
        config.setSources(List.of("amadeus")); // amadeus not registered (ObjectProvider returns nothing)
        AggregatorFlightClient noSourceAggregator = new AggregatorFlightClient(
                config, amadeusProvider, serpApiProvider, kiwiProvider, mockClientProvider);

        FlightSearchRequest request = validRequest();

        // serpapi and kiwi providers still have clients, but "amadeus" is not in the map
        // Since ObjectProvider.ifAvailable was already called in setUp(), serpapi/kiwi ARE registered
        // We need a fresh aggregator with only "amadeus" in sources list (none available)
        AggregatorConfig isolatedConfig = new AggregatorConfig();
        isolatedConfig.setEnabled(true);
        isolatedConfig.setSources(List.of("amadeus"));
        isolatedConfig.setTimeoutSeconds(5);
        isolatedConfig.setDeduplicateByFlightNumber(true);

        // Create a fresh aggregator where no providers register anything
        @SuppressWarnings("unchecked")
        ObjectProvider<AmadeusClientImpl> emptyAmadeus = org.mockito.Mockito.mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<SerpApiFlightClient> emptySerpApi = org.mockito.Mockito.mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<KiwiTequilaClient> emptyKiwi = org.mockito.Mockito.mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<AmadeusClientMock> emptyMock = org.mockito.Mockito.mock(ObjectProvider.class);

        AggregatorFlightClient emptyAggregator = new AggregatorFlightClient(
                isolatedConfig, emptyAmadeus, emptySerpApi, emptyKiwi, emptyMock);

        List<FlightOfferResponse> results = emptyAggregator.searchFlights(request);

        assertThat(results).isEmpty();
    }

    private FlightSearchRequest validRequest() {
        return new FlightSearchRequest("GRU", "LIS", LocalDate.of(2026, 4, 15), null, 1, CabinClass.ECONOMY);
    }

    private FlightOfferResponse buildOffer(String airline, double price, String source) {
        return new FlightOfferResponse(
                UUID.randomUUID(),
                "GRU",
                "LIS",
                LocalDate.of(2026, 4, 15),
                null,
                airline,
                BigDecimal.valueOf(price),
                "BRL",
                0,
                "12h 0m",
                CabinClass.ECONOMY,
                null,
                LocalDateTime.now(),
                source
        );
    }
}
