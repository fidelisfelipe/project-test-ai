package com.flightmonitor.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for creating a price alert.
 */
public record CreateAlertRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email address")
        String userEmail,

        @NotBlank(message = "Origin is required")
        @Pattern(regexp = "[A-Z]{3}", message = "Origin must be a 3-letter IATA code")
        String origin,

        @NotBlank(message = "Destination is required")
        @Pattern(regexp = "[A-Z]{3}", message = "Destination must be a 3-letter IATA code")
        String destination,

        @NotNull(message = "Departure date is required")
        @Future(message = "Departure date must be in the future")
        LocalDate departureDate,

        @NotNull(message = "Target price is required")
        @DecimalMin(value = "0.01", message = "Target price must be positive")
        BigDecimal targetPrice
) {
}
