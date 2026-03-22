package com.flightmonitor.messaging;

import com.flightmonitor.messaging.dto.FlightSearchRequestMessage;
import com.flightmonitor.messaging.dto.FlightSearchResultMessage;
import com.flightmonitor.messaging.dto.PriceAlertMessage;

/**
 * Callback interface used by SyncMessageBus and real consumers
 * to process received messages.
 */
public interface MessageHandler {

    /**
     * Handles a flight search request message.
     *
     * @param message the search request
     * @return the search results
     * @throws Exception if processing fails
     */
    FlightSearchResultMessage handleSearchRequest(FlightSearchRequestMessage message) throws Exception;

    /**
     * Handles a price alert message.
     *
     * @param message the price alert
     * @throws Exception if processing fails
     */
    void handlePriceAlert(PriceAlertMessage message) throws Exception;
}
