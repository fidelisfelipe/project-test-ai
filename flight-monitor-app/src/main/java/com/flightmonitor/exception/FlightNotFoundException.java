package com.flightmonitor.exception;

/**
 * Exception thrown when a flight is not found.
 */
public class FlightNotFoundException extends RuntimeException {

    public FlightNotFoundException(String message) {
        super(message);
    }

    public FlightNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
