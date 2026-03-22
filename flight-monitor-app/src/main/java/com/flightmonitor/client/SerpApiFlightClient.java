package com.flightmonitor.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.flightmonitor.client.config.SerpApiConfig;
import com.flightmonitor.domain.enums.CabinClass;
import com.flightmonitor.dto.request.FlightSearchRequest;
import com.flightmonitor.dto.response.FlightOfferResponse;
import com.flightmonitor.exception.AmadeusApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Flight client that queries the SerpApi Google Flights engine.
 */
@Component
@ConditionalOnProperty(prefix = "serpapi", name = "api-key", matchIfMissing = false)
public class SerpApiFlightClient implements AmadeusClient {

    private static final Logger log = LoggerFactory.getLogger(SerpApiFlightClient.class);
    private static final DateTimeFormatter SERPAPI_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SerpApiConfig config;
    private final RestTemplate restTemplate;

    public SerpApiFlightClient(SerpApiConfig config, RestTemplate restTemplate) {
        this.config = config;
        this.restTemplate = restTemplate;
    }

    @Override
    public FlightSource getSource() {
        return FlightSource.SERPAPI_GOOGLE_FLIGHTS;
    }

    @Override
    public List<FlightOfferResponse> searchFlights(FlightSearchRequest request) {
        log.info("[SerpApi] Searching {} -> {} on {}", request.origin(), request.destination(), request.departureDate());

        String type = request.returnDate() != null ? "1" : "2";
        String url = UriComponentsBuilder.fromHttpUrl(config.getBaseUrl())
                .queryParam("engine", "google_flights")
                .queryParam("departure_id", request.origin())
                .queryParam("arrival_id", request.destination())
                .queryParam("outbound_date", request.departureDate().toString())
                .queryParam("type", type)
                .queryParam("adults", request.adults())
                .queryParam("travel_class", mapCabinClass(request.cabinClass()))
                .queryParam("currency", config.getCurrency())
                .queryParam("hl", config.getLanguage())
                .queryParam("no_cache", "false")
                .queryParam("api_key", config.getApiKey())
                .build()
                .toUriString();

        SerpApiResponse response;
        try {
            response = restTemplate.exchange(url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<SerpApiResponse>() {}).getBody();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new AmadeusApiException("SerpApi: API key inválida ou expirada");
            }
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                throw new AmadeusApiException("SerpApi: rate limit atingido (250/mês no free tier)");
            }
            throw new AmadeusApiException("SerpApi: HTTP error " + e.getStatusCode() + ": " + e.getMessage(), e);
        }

        if (response == null) {
            return List.of();
        }

        List<SerpApiOffer> allOffers = new ArrayList<>();
        if (response.bestFlights() != null) {
            allOffers.addAll(response.bestFlights());
        }
        if (response.otherFlights() != null) {
            allOffers.addAll(response.otherFlights());
        }

        log.info("[SerpApi] Found {} results ({} best + {} other)",
                allOffers.size(),
                response.bestFlights() != null ? response.bestFlights().size() : 0,
                response.otherFlights() != null ? response.otherFlights().size() : 0);

        if (response.priceInsights() != null && response.priceInsights().priceLevel() != null
                && response.priceInsights().priceLevel().contains("high")) {
            log.warn("[SerpApi] Price insights indicate prices are above typical range for this route");
        }

        List<FlightOfferResponse> results = allOffers.stream()
                .filter(offer -> offer.flights() != null && !offer.flights().isEmpty())
                .map(offer -> mapToResponse(offer, request))
                .sorted(Comparator.comparing(FlightOfferResponse::price))
                .limit(config.getMaxResults())
                .toList();

        return results;
    }

    private FlightOfferResponse mapToResponse(SerpApiOffer offer, FlightSearchRequest request) {
        SerpApiLeg firstLeg = offer.flights().get(0);
        SerpApiLeg lastLeg = offer.flights().get(offer.flights().size() - 1);

        String origin = firstLeg.departureAirport() != null ? firstLeg.departureAirport().id() : request.origin();
        String destination = lastLeg.arrivalAirport() != null ? lastLeg.arrivalAirport().id() : request.destination();
        String airline = firstLeg.airline() != null ? firstLeg.airline() : "";
        String flightNumber = firstLeg.flightNumber() != null ? firstLeg.flightNumber() : "";
        BigDecimal price = offer.price() != null ? BigDecimal.valueOf(offer.price()) : BigDecimal.ZERO;
        int stops = offer.flights().size() - 1;
        String duration = formatDuration(offer.totalDuration());
        CabinClass cabinClass = mapTravelClass(firstLeg.travelClass());

        LocalDate departureDate = request.departureDate();
        if (firstLeg.departureAirport() != null && firstLeg.departureAirport().time() != null) {
            try {
                departureDate = LocalDate.parse(firstLeg.departureAirport().time(), SERPAPI_DATE_FMT);
            } catch (Exception ignored) {
                // fallback to request date
            }
        }

        return new FlightOfferResponse(
                UUID.randomUUID(),
                origin,
                destination,
                departureDate,
                request.returnDate(),
                airline,
                price,
                config.getCurrency(),
                stops,
                duration,
                cabinClass,
                null,
                LocalDateTime.now(),
                FlightSource.SERPAPI_GOOGLE_FLIGHTS.name()
        );
    }

    private String formatDuration(Integer totalMinutes) {
        if (totalMinutes == null) return "";
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        return hours + "h " + minutes + "m";
    }

    private int mapCabinClass(CabinClass cabin) {
        if (cabin == null) return 1;
        return switch (cabin) {
            case ECONOMY -> 1;
            case PREMIUM_ECONOMY -> 2;
            case BUSINESS -> 3;
            case FIRST -> 4;
        };
    }

    private CabinClass mapTravelClass(String travelClass) {
        if (travelClass == null) return CabinClass.ECONOMY;
        return switch (travelClass.toLowerCase()) {
            case "business" -> CabinClass.BUSINESS;
            case "first", "first class" -> CabinClass.FIRST;
            case "premium economy", "premium_economy" -> CabinClass.PREMIUM_ECONOMY;
            default -> CabinClass.ECONOMY;
        };
    }

    // ---- Internal DTOs ----

    record SerpApiResponse(
            @JsonProperty("best_flights") List<SerpApiOffer> bestFlights,
            @JsonProperty("other_flights") List<SerpApiOffer> otherFlights,
            @JsonProperty("price_insights") SerpApiPriceInsights priceInsights
    ) {}

    record SerpApiOffer(
            List<SerpApiLeg> flights,
            Integer price,
            @JsonProperty("total_duration") Integer totalDuration,
            String type,
            @JsonProperty("airline_logo") String airlineLogo
    ) {}

    record SerpApiLeg(
            @JsonProperty("departure_airport") SerpApiAirport departureAirport,
            @JsonProperty("arrival_airport") SerpApiAirport arrivalAirport,
            Integer duration,
            String airline,
            @JsonProperty("flight_number") String flightNumber,
            @JsonProperty("travel_class") String travelClass,
            String airplane
    ) {}

    record SerpApiAirport(String name, String id, String time) {}

    record SerpApiPriceInsights(
            @JsonProperty("lowest_price") Integer lowestPrice,
            @JsonProperty("price_level") String priceLevel,
            @JsonProperty("typical_price_range") List<Integer> typicalPriceRange
    ) {}
}
