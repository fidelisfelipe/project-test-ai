package com.flightmonitor.client;

import com.amadeus.Amadeus;
import com.amadeus.Params;
import com.amadeus.exceptions.ResponseException;
import com.amadeus.resources.FlightOfferSearch;
import com.flightmonitor.config.AmadeusConfig;
import com.flightmonitor.domain.enums.CabinClass;
import com.flightmonitor.dto.request.FlightSearchRequest;
import com.flightmonitor.dto.response.FlightOfferResponse;
import com.flightmonitor.exception.AmadeusApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of the AmadeusClient that calls the Amadeus flight search API.
 */
@Component
@Profile("!sync & !test")
public class AmadeusClientImpl implements AmadeusClient {

    private static final Logger log = LoggerFactory.getLogger(AmadeusClientImpl.class);

    private final Amadeus amadeus;

    public AmadeusClientImpl(AmadeusConfig amadeusConfig) {
        this.amadeus = Amadeus.builder(amadeusConfig.getKey(), amadeusConfig.getSecret())
                .setHostname(amadeusConfig.getHost())
                .build();
    }

    /**
     * Searches for flight offers using the Amadeus API.
     *
     * @param request the search parameters
     * @return list of matching flight offers
     */
    @Override
    public List<FlightOfferResponse> searchFlights(FlightSearchRequest request) {
        log.info("Calling Amadeus API for route {}->{} on {}", request.origin(), request.destination(), request.departureDate());
        try {
            Params params = Params
                    .with("originLocationCode", request.origin())
                    .and("destinationLocationCode", request.destination())
                    .and("departureDate", request.departureDate().toString())
                    .and("adults", String.valueOf(request.adults()))
                    .and("max", "20")
                    .and("currencyCode", "EUR");

            if (request.returnDate() != null) {
                params.and("returnDate", request.returnDate().toString());
            }
            if (request.cabinClass() != null) {
                params.and("travelClass", request.cabinClass().name());
            }

            FlightOfferSearch[] offers = amadeus.shopping.flightOffersSearch.get(params);
            List<FlightOfferResponse> results = new ArrayList<>();

            if (offers != null) {
                for (FlightOfferSearch offer : offers) {
                    FlightOfferResponse response = mapToResponse(offer, request);
                    results.add(response);
                }
            }

            log.info("Amadeus API returned {} offers for route {}->{}", results.size(), request.origin(), request.destination());
            return results;
        } catch (ResponseException e) {
            log.error("Amadeus API error for route {}->{}: {}", request.origin(), request.destination(), e.getMessage());
            throw new AmadeusApiException("Amadeus API error: " + e.getMessage(), e);
        }
    }

    private FlightOfferResponse mapToResponse(FlightOfferSearch offer, FlightSearchRequest request) {
        BigDecimal price = BigDecimal.ZERO;
        if (offer.getPrice() != null && offer.getPrice().getTotal() != null) {
            price = new BigDecimal(offer.getPrice().getTotal());
        }

        String airline = "";
        int stops = 0;
        String duration = "";
        if (offer.getItineraries() != null && offer.getItineraries().length > 0) {
            FlightOfferSearch.Itinerary itinerary = offer.getItineraries()[0];
            duration = itinerary.getDuration() != null ? itinerary.getDuration() : "";
            if (itinerary.getSegments() != null && itinerary.getSegments().length > 0) {
                stops = itinerary.getSegments().length - 1;
                airline = itinerary.getSegments()[0].getCarrierCode() != null
                        ? itinerary.getSegments()[0].getCarrierCode() : "";
            }
        }

        CabinClass cabinClass = request.cabinClass() != null ? request.cabinClass() : CabinClass.ECONOMY;

        return new FlightOfferResponse(
                UUID.randomUUID(),
                request.origin(),
                request.destination(),
                request.departureDate(),
                request.returnDate(),
                airline,
                price,
                "EUR",
                stops,
                duration,
                cabinClass,
                null,
                LocalDateTime.now(),
                "AMADEUS"
        );
    }
}
