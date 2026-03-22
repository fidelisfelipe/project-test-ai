package com.flightmonitor.controller;

import com.flightmonitor.domain.enums.CabinClass;
import com.flightmonitor.dto.request.FlightSearchRequest;
import com.flightmonitor.dto.response.FlightOfferResponse;
import com.flightmonitor.dto.response.FlightReportResponse;
import com.flightmonitor.dto.response.PriceHistoryResponse;
import com.flightmonitor.service.FlightSearchService;
import com.flightmonitor.service.PriceHistoryService;
import com.flightmonitor.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for flight search, history, and report endpoints.
 */
@RestController
@RequestMapping("/api/v1/flights")
@Tag(name = "Flights", description = "Flight search and price history operations")
@Validated
public class FlightSearchController {

    private final FlightSearchService flightSearchService;
    private final PriceHistoryService priceHistoryService;
    private final ReportService reportService;

    public FlightSearchController(FlightSearchService flightSearchService,
                                  PriceHistoryService priceHistoryService,
                                  ReportService reportService) {
        this.flightSearchService = flightSearchService;
        this.priceHistoryService = priceHistoryService;
        this.reportService = reportService;
    }

    /**
     * Searches for available flight offers.
     *
     * @param origin        the origin IATA airport code
     * @param destination   the destination IATA airport code
     * @param departureDate the departure date (must be in the future)
     * @param returnDate    optional return date
     * @param adults        number of adult passengers (default 1)
     * @param cabinClass    cabin class selection
     * @return list of matching flight offers sorted by price
     */
    @GetMapping("/search")
    @Operation(summary = "Search for flight offers", description = "Returns available flight offers sorted by price ascending")
    public ResponseEntity<List<FlightOfferResponse>> searchFlights(
            @Parameter(description = "Origin IATA code (e.g., BSB)")
            @RequestParam @NotBlank @Pattern(regexp = "[A-Z]{3}") String origin,
            @Parameter(description = "Destination IATA code (e.g., LIS)")
            @RequestParam @NotBlank @Pattern(regexp = "[A-Z]{3}") String destination,
            @Parameter(description = "Departure date (YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Future LocalDate departureDate,
            @Parameter(description = "Return date (optional)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate returnDate,
            @Parameter(description = "Number of adult passengers")
            @RequestParam(defaultValue = "1") @Min(1) int adults,
            @Parameter(description = "Cabin class")
            @RequestParam(required = false) CabinClass cabinClass) {

        FlightSearchRequest request = new FlightSearchRequest(origin, destination, departureDate, returnDate, adults, cabinClass);
        List<FlightOfferResponse> results = flightSearchService.searchFlights(request);
        return ResponseEntity.ok(results);
    }

    /**
     * Retrieves price history for a given route over recent days.
     *
     * @param origin      the origin IATA code
     * @param destination the destination IATA code
     * @param days        number of days to look back (default 30)
     * @return list of price history entries
     */
    @GetMapping("/history")
    @Operation(summary = "Get price history", description = "Returns price history for a route over the specified number of days")
    public ResponseEntity<List<PriceHistoryResponse>> getHistory(
            @RequestParam @NotBlank @Pattern(regexp = "[A-Z]{3}") String origin,
            @RequestParam @NotBlank @Pattern(regexp = "[A-Z]{3}") String destination,
            @RequestParam(defaultValue = "30") int days) {

        List<PriceHistoryResponse> history = priceHistoryService.getHistory(origin, destination, days);
        return ResponseEntity.ok(history);
    }

    /**
     * Generates a price report for a route and date range.
     *
     * @param origin      the origin IATA code
     * @param destination the destination IATA code
     * @param startDate   report start date
     * @param endDate     report end date
     * @return the compiled flight report
     */
    @GetMapping("/report")
    @Operation(summary = "Generate price report", description = "Returns a comprehensive price report for a route")
    public ResponseEntity<FlightReportResponse> getReport(
            @RequestParam @NotBlank @Pattern(regexp = "[A-Z]{3}") String origin,
            @RequestParam @NotBlank @Pattern(regexp = "[A-Z]{3}") String destination,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        FlightReportResponse report = reportService.generateReport(origin, destination, startDate, endDate);
        return ResponseEntity.ok(report);
    }
}
