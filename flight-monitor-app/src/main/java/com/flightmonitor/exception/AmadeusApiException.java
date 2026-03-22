package com.flightmonitor.exception;

/**
 * Exception thrown when the Amadeus API returns an error.
 */
public class AmadeusApiException extends RuntimeException {

    public AmadeusApiException(String message) {
        super(message);
    }

    public AmadeusApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
