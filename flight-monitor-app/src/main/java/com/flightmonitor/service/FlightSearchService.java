package com.flightmonitor.service;

import com.flightmonitor.dto.request.FlightSearchRequest;
import com.flightmonitor.dto.response.FlightOfferResponse;

import java.util.List;

/**
 * Service interface for searching flight offers.
 */
public interface FlightSearchService {

    /**
     * Searches for available flight offers based on the provided criteria.
     *
     * @param request the search parameters
     * @return list of matching flight offers sorted by price ascending
     */
    List<FlightOfferResponse> searchFlights(FlightSearchRequest request);
}
