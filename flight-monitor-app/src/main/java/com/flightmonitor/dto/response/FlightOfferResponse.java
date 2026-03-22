package com.flightmonitor.dto.response;

import com.flightmonitor.domain.enums.CabinClass;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for a flight offer.
 */
public record FlightOfferResponse(
        UUID id,
        String origin,
        String destination,
        LocalDate departureDate,
        LocalDate returnDate,
        String airline,
        BigDecimal price,
        String currency,
        int stops,
        String duration,
        CabinClass cabinClass,
        String deepLink,
        LocalDateTime capturedAt,
        String source
) {
}
