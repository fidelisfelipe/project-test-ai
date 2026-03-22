package com.flightmonitor.messaging.dto;

import com.flightmonitor.dto.response.FlightOfferResponse;

import java.util.List;

/**
 * Message containing flight search results.
 */
public record FlightSearchResultMessage(
        String correlationId,
        List<FlightOfferResponse> offers
) {
}
