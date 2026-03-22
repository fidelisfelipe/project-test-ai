package com.flightmonitor.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO for a flight price report.
 */
public record FlightReportResponse(
        String origin,
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal lowestPrice,
        BigDecimal highestPrice,
        BigDecimal averagePrice,
        int totalSearches,
        String priceTrend,
        List<PriceHistoryResponse> history
) {
}
