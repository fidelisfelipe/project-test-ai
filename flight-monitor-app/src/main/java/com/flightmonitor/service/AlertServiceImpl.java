package com.flightmonitor.service;

import com.flightmonitor.domain.entity.PriceAlert;
import com.flightmonitor.domain.enums.AlertStatus;
import com.flightmonitor.dto.request.CreateAlertRequest;
import com.flightmonitor.dto.response.PriceAlertResponse;
import com.flightmonitor.exception.FlightNotFoundException;
import com.flightmonitor.mapper.PriceAlertMapper;
import com.flightmonitor.messaging.MessageBus;
import com.flightmonitor.messaging.dto.PriceAlertMessage;
import com.flightmonitor.repository.PriceAlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of the AlertService.
 */
@Service
public class AlertServiceImpl implements AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertServiceImpl.class);

    private final PriceAlertRepository alertRepository;
    private final PriceAlertMapper alertMapper;
    private final MessageBus messageBus;

    public AlertServiceImpl(PriceAlertRepository alertRepository,
                            PriceAlertMapper alertMapper,
                            MessageBus messageBus) {
        this.alertRepository = alertRepository;
        this.alertMapper = alertMapper;
        this.messageBus = messageBus;
    }

    /**
     * Creates a new price alert with ACTIVE status.
     *
     * @param request the alert creation parameters
     * @return the created alert response
     */
    @Override
    @Transactional
    public PriceAlertResponse createAlert(CreateAlertRequest request) {
        if (request.userEmail() == null || !request.userEmail().contains("@")) {
            throw new IllegalArgumentException("Invalid email address");
        }
        PriceAlert alert = new PriceAlert();
        alert.setUserEmail(request.userEmail());
        alert.setOrigin(request.origin());
        alert.setDestination(request.destination());
        alert.setDepartureDate(request.departureDate());
        alert.setTargetPrice(request.targetPrice());
        alert.setStatus(AlertStatus.ACTIVE);
        PriceAlert saved = alertRepository.save(alert);
        log.info("Created alert id={} for user={} route={}->{}", saved.getId(), saved.getUserEmail(), saved.getOrigin(), saved.getDestination());
        return alertMapper.toResponse(saved);
    }

    /**
     * Retrieves all alerts associated with a user's email.
     *
     * @param userEmail the user's email address
     * @return list of price alert responses
     */
    @Override
    public List<PriceAlertResponse> getAlertsByEmail(String userEmail) {
        return alertRepository.findByUserEmail(userEmail).stream()
                .map(alertMapper::toResponse)
                .toList();
    }

    /**
     * Deletes an alert by its UUID.
     *
     * @param id the alert UUID
     */
    @Override
    @Transactional
    public void deleteAlert(UUID id) {
        if (!alertRepository.existsById(id)) {
            throw new FlightNotFoundException("Alert not found with id: " + id);
        }
        alertRepository.deleteById(id);
        log.info("Deleted alert id={}", id);
    }

    /**
     * Pauses an active alert.
     *
     * @param id the alert UUID
     * @return the updated alert response
     */
    @Override
    @Transactional
    public PriceAlertResponse pauseAlert(UUID id) {
        PriceAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new FlightNotFoundException("Alert not found with id: " + id));
        alert.setStatus(AlertStatus.PAUSED);
        PriceAlert saved = alertRepository.save(alert);
        log.info("Paused alert id={}", id);
        return alertMapper.toResponse(saved);
    }

    /**
     * Resumes a paused alert.
     *
     * @param id the alert UUID
     * @return the updated alert response
     */
    @Override
    @Transactional
    public PriceAlertResponse resumeAlert(UUID id) {
        PriceAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new FlightNotFoundException("Alert not found with id: " + id));
        alert.setStatus(AlertStatus.ACTIVE);
        PriceAlert saved = alertRepository.save(alert);
        log.info("Resumed alert id={}", id);
        return alertMapper.toResponse(saved);
    }

    /**
     * Checks all active alerts for a given route and triggers those where the current price meets the target.
     *
     * @param origin          the origin IATA code
     * @param destination     the destination IATA code
     * @param currentMinPrice the current minimum price
     */
    @Override
    @Transactional
    public void checkAlertsForRoute(String origin, String destination, BigDecimal currentMinPrice) {
        List<PriceAlert> activeAlerts = alertRepository.findByOriginAndDestinationAndStatus(
                origin, destination, AlertStatus.ACTIVE);

        for (PriceAlert alert : activeAlerts) {
            alert.setLastCheckedAt(LocalDateTime.now());
            if (currentMinPrice.compareTo(alert.getTargetPrice()) <= 0) {
                alert.setStatus(AlertStatus.TRIGGERED);
                alert.setTriggeredAt(LocalDateTime.now());
                alertRepository.save(alert);
                PriceAlertMessage alertMessage = new PriceAlertMessage(
                        alert.getId(),
                        alert.getUserEmail(),
                        alert.getOrigin(),
                        alert.getDestination(),
                        alert.getDepartureDate(),
                        alert.getTargetPrice(),
                        currentMinPrice,
                        LocalDateTime.now()
                );
                try {
                    messageBus.sendPriceAlert(alertMessage).get(30, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.warn("Failed to send price alert alertId={}: {}", alert.getId(), e.getMessage());
                }
                log.info("Alert triggered id={} user={} price={} target={}", alert.getId(), alert.getUserEmail(), currentMinPrice, alert.getTargetPrice());
            } else {
                alertRepository.save(alert);
            }
        }
    }
}
