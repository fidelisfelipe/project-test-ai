package com.flightmonitor.messaging.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kafka message for a price alert notification.
 */
public record PriceAlertMessage(
        UUID alertId,
        String userEmail,
        String origin,
        String destination,
        LocalDate departureDate,
        BigDecimal targetPrice,
        BigDecimal currentPrice,
        LocalDateTime triggeredAt
) {
}
