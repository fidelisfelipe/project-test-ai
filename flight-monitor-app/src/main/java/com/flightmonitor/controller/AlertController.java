package com.flightmonitor.controller;

import com.flightmonitor.dto.request.CreateAlertRequest;
import com.flightmonitor.dto.response.PriceAlertResponse;
import com.flightmonitor.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for price alert management.
 */
@RestController
@RequestMapping("/api/v1/alerts")
@Tag(name = "Alerts", description = "Price alert management operations")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    /**
     * Creates a new price alert.
     *
     * @param request the alert creation parameters
     * @return 201 Created with Location header pointing to the new resource
     */
    @PostMapping
    @Operation(summary = "Create price alert", description = "Creates a new price alert for a specific route")
    public ResponseEntity<PriceAlertResponse> createAlert(@Valid @RequestBody CreateAlertRequest request) {
        PriceAlertResponse created = alertService.createAlert(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    /**
     * Retrieves all alerts for a given user email.
     *
     * @param email the user's email address
     * @return list of price alerts
     */
    @GetMapping
    @Operation(summary = "Get alerts by email", description = "Returns all price alerts for the specified email")
    public ResponseEntity<List<PriceAlertResponse>> getAlerts(@RequestParam String email) {
        List<PriceAlertResponse> alerts = alertService.getAlertsByEmail(email);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Deletes an alert by ID.
     *
     * @param id the alert UUID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete alert", description = "Deletes an alert by its ID")
    public ResponseEntity<Void> deleteAlert(@PathVariable UUID id) {
        alertService.deleteAlert(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Pauses an active alert.
     *
     * @param id the alert UUID
     * @return the updated alert
     */
    @PutMapping("/{id}/pause")
    @Operation(summary = "Pause alert", description = "Pauses an active price alert")
    public ResponseEntity<PriceAlertResponse> pauseAlert(@PathVariable UUID id) {
        PriceAlertResponse updated = alertService.pauseAlert(id);
        return ResponseEntity.ok(updated);
    }

    /**
     * Resumes a paused alert.
     *
     * @param id the alert UUID
     * @return the updated alert
     */
    @PutMapping("/{id}/resume")
    @Operation(summary = "Resume alert", description = "Resumes a paused price alert")
    public ResponseEntity<PriceAlertResponse> resumeAlert(@PathVariable UUID id) {
        PriceAlertResponse updated = alertService.resumeAlert(id);
        return ResponseEntity.ok(updated);
    }
}
