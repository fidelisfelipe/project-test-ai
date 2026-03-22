package com.flightmonitor.dto.request;

import com.flightmonitor.domain.enums.CabinClass;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

/**
 * Request DTO for searching flight offers.
 */
public record FlightSearchRequest(
        @NotBlank(message = "Origin is required")
        @Pattern(regexp = "[A-Z]{3}", message = "Origin must be a 3-letter IATA code")
        String origin,

        @NotBlank(message = "Destination is required")
        @Pattern(regexp = "[A-Z]{3}", message = "Destination must be a 3-letter IATA code")
        String destination,

        @NotNull(message = "Departure date is required")
        @Future(message = "Departure date must be in the future")
        LocalDate departureDate,

        LocalDate returnDate,

        @Min(value = 1, message = "At least 1 adult is required")
        int adults,

        CabinClass cabinClass
) {
    public FlightSearchRequest {
        if (adults < 1) {
            adults = 1;
        }
        if (cabinClass == null) {
            cabinClass = CabinClass.ECONOMY;
        }
    }
}
