package com.flightmonitor.dto.response;

import com.flightmonitor.domain.enums.AlertStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for a price alert.
 */
public record PriceAlertResponse(
        UUID id,
        String userEmail,
        String origin,
        String destination,
        LocalDate departureDate,
        BigDecimal targetPrice,
        AlertStatus status,
        LocalDateTime triggeredAt,
        LocalDateTime createdAt,
        LocalDateTime lastCheckedAt
) {
}
