package com.flightmonitor.mapper;

import com.flightmonitor.domain.entity.FlightOffer;
import com.flightmonitor.dto.response.FlightOfferResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting FlightOffer entities to response DTOs.
 */
@Component
public class FlightOfferMapper {

    /**
     * Converts a FlightOffer entity to a FlightOfferResponse DTO.
     *
     * @param offer the entity to convert
     * @return the response DTO
     */
    public FlightOfferResponse toResponse(FlightOffer offer) {
        return new FlightOfferResponse(
                offer.getId(),
                offer.getOrigin(),
                offer.getDestination(),
                offer.getDepartureDate(),
                offer.getReturnDate(),
                offer.getAirline(),
                offer.getPrice(),
                offer.getCurrency(),
                offer.getStops(),
                offer.getDuration(),
                offer.getCabinClass(),
                offer.getDeepLink(),
                offer.getCapturedAt(),
                offer.getSource()
        );
    }
}
