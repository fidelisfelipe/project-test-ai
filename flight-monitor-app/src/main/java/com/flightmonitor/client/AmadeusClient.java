package com.flightmonitor.client;

import com.flightmonitor.dto.request.FlightSearchRequest;
import com.flightmonitor.dto.response.FlightOfferResponse;

import java.util.List;

/**
 * Client interface for the Amadeus flight search API.
 */
public interface AmadeusClient {

    /**
     * Searches for available flight offers using the Amadeus API.
     *
     * @param request the search parameters
     * @return list of matching flight offers
     */
    List<FlightOfferResponse> searchFlights(FlightSearchRequest request);

    /**
     * Returns the flight data source identifier for this client.
     *
     * @return the source of flight data
     */
    default FlightSource getSource() {
        return FlightSource.AMADEUS;
    }
}
