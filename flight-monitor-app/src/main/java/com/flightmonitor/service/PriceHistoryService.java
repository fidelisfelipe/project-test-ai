package com.flightmonitor.service;

import com.flightmonitor.dto.response.FlightReportResponse;
import com.flightmonitor.dto.response.PriceHistoryResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for price history operations.
 */
public interface PriceHistoryService {

    /**
     * Retrieves the price history for a given route over the last N days.
     *
     * @param origin      the origin IATA code
     * @param destination the destination IATA code
     * @param days        number of days to look back
     * @return list of price history entries sorted by recordedAt descending
     */
    List<PriceHistoryResponse> getHistory(String origin, String destination, int days);

    /**
     * Saves a price snapshot for a given route.
     *
     * @param origin        the origin IATA code
     * @param destination   the destination IATA code
     * @param departureDate the departure date
     * @param offers        list of offer responses to compute min/max/avg from
     */
    void saveSnapshot(String origin, String destination, LocalDate departureDate,
                      List<com.flightmonitor.dto.response.FlightOfferResponse> offers);

    /**
     * Calculates the price trend given a list of ordered history entries.
     *
     * @param history ordered price history records
     * @return "UP", "DOWN", or "STABLE"
     */
    String calculateTrend(List<PriceHistoryResponse> history);
}
