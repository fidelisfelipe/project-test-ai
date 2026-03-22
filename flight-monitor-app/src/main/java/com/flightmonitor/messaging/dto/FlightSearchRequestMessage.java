package com.flightmonitor.messaging.dto;

import com.flightmonitor.domain.enums.CabinClass;

import java.time.LocalDate;

/**
 * Kafka message for a flight search request.
 */
public record FlightSearchRequestMessage(
        String correlationId,
        String origin,
        String destination,
        LocalDate departureDate,
        LocalDate returnDate,
        int adults,
        CabinClass cabinClass
) {
}
