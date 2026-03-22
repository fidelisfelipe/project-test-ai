package com.flightmonitor.unit.client;

import com.flightmonitor.client.KiwiTequilaClient;
import com.flightmonitor.client.config.KiwiConfig;
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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KiwiTequilaClientTest {

    private MockRestServiceServer mockServer;
    private KiwiTequilaClient client;
    private KiwiConfig config;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        config = new KiwiConfig();
        config.setApiKey("test-kiwi-key");
        config.setPartner("picky");
        config.setCurrency("BRL");
        config.setLocale("pt-BR");
        config.setBaseUrl("https://tequila-api.kiwi.com");
        config.setLimit(20);
        client = new KiwiTequilaClient(config, restTemplate);
    }

    @Test
    void searchFlights_mapsDirectItinerary_zeroStops() throws IOException {
        String json = loadFixture("/fixtures/kiwi_response.json");
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("tequila-api.kiwi.com")))
                .andExpect(header("apikey", "test-kiwi-key"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        FlightSearchRequest request = new FlightSearchRequest(
                "GRU", "LIS", LocalDate.of(2026, 4, 15), null, 1, CabinClass.ECONOMY);

        List<FlightOfferResponse> results = client.searchFlights(request);

        assertThat(results).hasSize(1);
        FlightOfferResponse offer = results.get(0);
        assertThat(offer.stops()).isEqualTo(0);
        assertThat(offer.deepLink()).isNotNull().contains("kiwi.com");
        assertThat(offer.airline()).isEqualTo("TP");
        assertThat(offer.origin()).isEqualTo("GRU");
        assertThat(offer.destination()).isEqualTo("LIS");
        assertThat(offer.currency()).isEqualTo("BRL");
    }

    @Test
    void searchFlights_mapsItineraryWithConnection_oneStop() throws IOException {
        String json = """
                {
                  "data": [
                    {
                      "id": "multi123",
                      "flyFrom": "GRU",
                      "flyTo": "LIS",
                      "cityFrom": "São Paulo",
                      "cityTo": "Lisboa",
                      "airlines": ["TP", "IB"],
                      "price": 3200.0,
                      "currency": "BRL",
                      "duration": { "departure": 56700, "return": 0, "total": 56700 },
                      "route": [
                        {
                          "flyFrom": "GRU",
                          "flyTo": "MAD",
                          "airline": "IB",
                          "flight_no": 6832,
                          "local_departure": "2026-04-15T14:00:00.000Z",
                          "local_arrival": "2026-04-16T04:00:00.000Z",
                          "equipment": "789"
                        },
                        {
                          "flyFrom": "MAD",
                          "flyTo": "LIS",
                          "airline": "IB",
                          "flight_no": 3102,
                          "local_departure": "2026-04-16T07:00:00.000Z",
                          "local_arrival": "2026-04-16T08:05:00.000Z",
                          "equipment": "320"
                        }
                      ],
                      "deep_link": "https://www.kiwi.com/deep?flightsId=multi123"
                    }
                  ],
                  "currency": "BRL",
                  "_results": 1
                }
                """;
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("tequila-api.kiwi.com")))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        FlightSearchRequest request = new FlightSearchRequest(
                "GRU", "LIS", LocalDate.of(2026, 4, 15), null, 1, CabinClass.ECONOMY);

        List<FlightOfferResponse> results = client.searchFlights(request);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).stops()).isEqualTo(1);
        assertThat(results.get(0).airline()).isEqualTo("TP/IB");
    }

    @Test
    void searchFlights_returnsEmptyListWhenNoResults() {
        String json = """
                { "data": [], "_results": 0, "currency": "BRL" }
                """;
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("tequila-api.kiwi.com")))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        FlightSearchRequest request = new FlightSearchRequest(
                "GRU", "LIS", LocalDate.of(2026, 4, 15), null, 1, CabinClass.ECONOMY);

        List<FlightOfferResponse> results = client.searchFlights(request);

        assertThat(results).isEmpty();
    }

    @Test
    void searchFlights_throwsDescriptiveExceptionOn403_unknownPartner() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("tequila-api.kiwi.com")))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .body("unknown partner provided")
                        .contentType(MediaType.TEXT_PLAIN));

        FlightSearchRequest request = new FlightSearchRequest(
                "GRU", "LIS", LocalDate.of(2026, 4, 15), null, 1, CabinClass.ECONOMY);

        assertThatThrownBy(() -> client.searchFlights(request))
                .isInstanceOf(AmadeusApiException.class)
                .hasMessageContaining("parceiro inválido");
    }

    private String loadFixture(String path) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            assertThat(is).isNotNull();
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
