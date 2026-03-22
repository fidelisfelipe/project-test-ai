package com.flightmonitor.messaging;

import com.flightmonitor.messaging.dto.FlightSearchRequestMessage;
import com.flightmonitor.messaging.dto.PriceAlertMessage;

import java.util.concurrent.CompletableFuture;

/**
 * Central contract for sending messages.
 * All implementations must be thread-safe.
 */
public interface MessageBus {

    /**
     * Sends a flight search request message.
     * In sync mode: processes immediately in the same thread.
     * In async mode: publishes to the configured broker.
     *
     * @param message the search request
     * @return CompletableFuture that completes when the message is accepted
     */
    CompletableFuture<MessageSendResult> sendSearchRequest(FlightSearchRequestMessage message);

    /**
     * Sends a price alert notification message.
     *
     * @param message the price alert
     * @return CompletableFuture that completes when the message is accepted
     */
    CompletableFuture<MessageSendResult> sendPriceAlert(PriceAlertMessage message);

    /**
     * Returns the active broker type for health checks and admin display.
     *
     * @return the broker type
     */
    BrokerType getBrokerType();

    /**
     * Checks if the broker is reachable.
     * Sync always returns true.
     *
     * @return true if the broker is available
     */
    boolean isAvailable();
}
