package com.flightmonitor.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for price history data.
 */
public record PriceHistoryResponse(
        UUID id,
        String origin,
        String destination,
        LocalDate departureDate,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        BigDecimal avgPrice,
        int offersCount,
        LocalDateTime recordedAt,
        String trend
) {
}
