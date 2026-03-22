package com.flightmonitor.service;

import com.flightmonitor.dto.request.CreateAlertRequest;
import com.flightmonitor.dto.response.PriceAlertResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing price alerts.
 */
public interface AlertService {

    /**
     * Creates a new price alert.
     *
     * @param request the alert creation parameters
     * @return the created alert response
     */
    PriceAlertResponse createAlert(CreateAlertRequest request);

    /**
     * Retrieves all alerts for a given user email.
     *
     * @param userEmail the user's email address
     * @return list of alerts belonging to the user
     */
    List<PriceAlertResponse> getAlertsByEmail(String userEmail);

    /**
     * Deletes an alert by its ID.
     *
     * @param id the alert UUID
     */
    void deleteAlert(UUID id);

    /**
     * Pauses an active alert.
     *
     * @param id the alert UUID
     * @return the updated alert response
     */
    PriceAlertResponse pauseAlert(UUID id);

    /**
     * Resumes a paused alert.
     *
     * @param id the alert UUID
     * @return the updated alert response
     */
    PriceAlertResponse resumeAlert(UUID id);

    /**
     * Checks all active alerts for a given route and triggers those where current price meets the target.
     *
     * @param origin         the origin IATA code
     * @param destination    the destination IATA code
     * @param currentMinPrice the current minimum price found
     */
    void checkAlertsForRoute(String origin, String destination, BigDecimal currentMinPrice);
}
