package com.flightmonitor.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.flightmonitor.client.config.KiwiConfig;
import com.flightmonitor.domain.enums.CabinClass;
import com.flightmonitor.dto.request.FlightSearchRequest;
import com.flightmonitor.dto.response.FlightOfferResponse;
import com.flightmonitor.exception.AmadeusApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Flight client that queries the Kiwi Tequila API.
 */
@Component
@ConditionalOnProperty(prefix = "kiwi", name = "api-key", matchIfMissing = false)
public class KiwiTequilaClient implements AmadeusClient {

    private static final Logger log = LoggerFactory.getLogger(KiwiTequilaClient.class);
    private static final DateTimeFormatter KIWI_DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final KiwiConfig config;
    private final RestTemplate restTemplate;

    public KiwiTequilaClient(KiwiConfig config, RestTemplate restTemplate) {
        this.config = config;
        this.restTemplate = restTemplate;
    }

    @Override
    public FlightSource getSource() {
        return FlightSource.KIWI_TEQUILA;
    }

    @Override
    public List<FlightOfferResponse> searchFlights(FlightSearchRequest request) {
        log.info("[Kiwi] Searching {} -> {} on {} ({} adults)",
                request.origin(), request.destination(), request.departureDate(), request.adults());

        String dateFormatted = request.departureDate().format(KIWI_DATE_FMT);

        String url = UriComponentsBuilder.fromHttpUrl(config.getBaseUrl() + "/v2/search")
                .queryParam("fly_from", request.origin())
                .queryParam("fly_to", request.destination())
                .queryParam("dateFrom", dateFormatted)
                .queryParam("dateTo", dateFormatted)
                .queryParam("adults", request.adults())
                .queryParam("selected_cabins", mapCabin(request.cabinClass()))
                .queryParam("curr", config.getCurrency())
                .queryParam("locale", config.getLocale())
                .queryParam("partner", config.getPartner())
                .queryParam("limit", config.getLimit())
                .queryParam("sort", "price")
                .build()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", config.getApiKey());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        KiwiResponse response;
        try {
            response = restTemplate.exchange(url, HttpMethod.GET, entity, KiwiResponse.class).getBody();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                String body = e.getResponseBodyAsString();
                if (body.contains("unknown partner")) {
                    throw new AmadeusApiException("Kiwi: parceiro inválido — registre-se em tequila.kiwi.com");
                }
                throw new AmadeusApiException("Kiwi: API key ausente ou inválida");
            }
            throw new AmadeusApiException("Kiwi: HTTP error " + e.getStatusCode() + ": " + e.getMessage(), e);
        }

        if (response == null || response.data() == null || response.data().isEmpty()) {
            return List.of();
        }

        List<FlightOfferResponse> results = response.data().stream()
                .map(itinerary -> mapToResponse(itinerary, request, response.currency()))
                .sorted(Comparator.comparing(FlightOfferResponse::price))
                .toList();

        FlightOfferResponse cheapest = results.get(0);
        log.info("[Kiwi] Found {} results, cheapest: {} {}", results.size(), cheapest.price(), cheapest.currency());
        log.debug("[Kiwi] Cheapest deep link: {}", cheapest.deepLink());

        return results;
    }

    private FlightOfferResponse mapToResponse(KiwiItinerary itinerary, FlightSearchRequest request, String responseCurrency) {
        String id = "KIWI-" + itinerary.id();
        String airline = itinerary.airlines() != null ? String.join("/", itinerary.airlines()) : "";
        BigDecimal price = itinerary.price() != null ? BigDecimal.valueOf(itinerary.price()) : BigDecimal.ZERO;
        String currency = responseCurrency != null ? responseCurrency : config.getCurrency();
        int stops = itinerary.route() != null ? itinerary.route().size() - 1 : 0;
        String duration = formatDuration(itinerary.duration());
        CabinClass cabinClass = request.cabinClass() != null ? request.cabinClass() : CabinClass.ECONOMY;

        return new FlightOfferResponse(
                UUID.nameUUIDFromBytes(id.getBytes()),
                itinerary.flyFrom(),
                itinerary.flyTo(),
                request.departureDate(),
                request.returnDate(),
                airline,
                price,
                currency,
                stops,
                duration,
                cabinClass,
                itinerary.deepLink(),
                LocalDateTime.now(),
                FlightSource.KIWI_TEQUILA.name()
        );
    }

    private String formatDuration(KiwiDuration duration) {
        if (duration == null || duration.departure() == null) return "";
        long totalMinutes = duration.departure() / 60;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return hours + "h " + minutes + "m";
    }

    private String mapCabin(CabinClass cabin) {
        if (cabin == null) return "M";
        return switch (cabin) {
            case ECONOMY -> "M";
            case PREMIUM_ECONOMY -> "W";
            case BUSINESS -> "C";
            case FIRST -> "F";
        };
    }

    // ---- Internal DTOs ----

    record KiwiResponse(
            List<KiwiItinerary> data,
            String currency,
            @JsonProperty("_results") Integer results
    ) {}

    record KiwiItinerary(
            String id,
            String flyFrom,
            String flyTo,
            String cityFrom,
            String cityTo,
            List<String> airlines,
            Double price,
            KiwiDuration duration,
            List<KiwiRoute> route,
            @JsonProperty("deep_link") String deepLink
    ) {}

    record KiwiDuration(
            Long departure,
            @JsonProperty("return") Long returnDuration,
            Long total
    ) {}

    record KiwiRoute(
            String flyFrom,
            String flyTo,
            String airline,
            @JsonProperty("flight_no") Integer flightNo,
            @JsonProperty("local_departure") String localDeparture,
            @JsonProperty("local_arrival") String localArrival,
            String equipment
    ) {}
}
