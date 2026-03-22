package com.flightmonitor.unit.client;

import com.flightmonitor.client.SerpApiFlightClient;
import com.flightmonitor.client.config.SerpApiConfig;
import com.flightmonitor.domain.enums.CabinClass;
import com.flightmonitor.dto.request.FlightSearchRequest;
import com.flightmonitor.dto.response.FlightOfferResponse;
import com.flightmonitor.exception.AmadeusApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SerpApiFlightClientTest {

    private MockRestServiceServer mockServer;
    private SerpApiFlightClient client;
    private SerpApiConfig config;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        config = new SerpApiConfig();
        config.setApiKey("test-api-key");
        config.setCurrency("BRL");
        config.setLanguage("pt-br");
        config.setBaseUrl("https://serpapi.com/search");
        config.setMaxResults(20);
        client = new SerpApiFlightClient(config, restTemplate);
    }

    @Test
    void searchFlights_mapsBestFlightsAndOtherFlightsCorrectly() throws IOException {
        String json = loadFixture("/fixtures/serpapi_response.json");
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("serpapi.com")))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        FlightSearchRequest request = new FlightSearchRequest(
                "GRU", "LIS", LocalDate.of(2026, 4, 15), null, 1, CabinClass.ECONOMY);

        List<FlightOfferResponse> results = client.searchFlights(request);

        assertThat(results).hasSize(2);
        // Sorted by price: 2850 first, 3200 second
        assertThat(results.get(0).price().intValue()).isEqualTo(2850);
        assertThat(results.get(1).price().intValue()).isEqualTo(3200);
        // First result is direct (1 leg = 0 stops)
        assertThat(results.get(0).stops()).isEqualTo(0);
        // Second result has connection (2 legs = 1 stop)
        assertThat(results.get(1).stops()).isEqualTo(1);
    }

    @Test
    void searchFlights_returnsEmptyListWhenNullBody() throws IOException {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("serpapi.com")))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        FlightSearchRequest request = new FlightSearchRequest(
                "GRU", "LIS", LocalDate.of(2026, 4, 15), null, 1, CabinClass.ECONOMY);

        List<FlightOfferResponse> results = client.searchFlights(request);

        assertThat(results).isEmpty();
    }

    @Test
    void searchFlights_throwsExceptionOn429() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("serpapi.com")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        FlightSearchRequest request = new FlightSearchRequest(
                "GRU", "LIS", LocalDate.of(2026, 4, 15), null, 1, CabinClass.ECONOMY);

        assertThatThrownBy(() -> client.searchFlights(request))
                .isInstanceOf(AmadeusApiException.class)
                .hasMessageContaining("rate limit");
    }

    @Test
    void searchFlights_throwsExceptionOn401() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("serpapi.com")))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        FlightSearchRequest request = new FlightSearchRequest(
                "GRU", "LIS", LocalDate.of(2026, 4, 15), null, 1, CabinClass.ECONOMY);

        assertThatThrownBy(() -> client.searchFlights(request))
                .isInstanceOf(AmadeusApiException.class)
                .hasMessageContaining("API key");
    }

    private String loadFixture(String path) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            assertThat(is).isNotNull();
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

