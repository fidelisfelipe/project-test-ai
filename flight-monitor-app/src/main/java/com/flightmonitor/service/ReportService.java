package com.flightmonitor.service;

import com.flightmonitor.dto.response.FlightReportResponse;

import java.time.LocalDate;

/**
 * Service interface for generating flight price reports.
 */
public interface ReportService {

    /**
     * Generates a price report for a given route and date range.
     *
     * @param origin      the origin IATA code
     * @param destination the destination IATA code
     * @param startDate   the start date of the report range
     * @param endDate     the end date of the report range
     * @return a compiled flight report response
     */
    FlightReportResponse generateReport(String origin, String destination, LocalDate startDate, LocalDate endDate);
}
