package com.flightmonitor.repository;

import com.flightmonitor.domain.entity.PriceAlert;
import com.flightmonitor.domain.enums.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for PriceAlert entities.
 */
@Repository
public interface PriceAlertRepository extends JpaRepository<PriceAlert, UUID> {

    List<PriceAlert> findByUserEmail(String userEmail);

    List<PriceAlert> findByStatus(AlertStatus status);

    List<PriceAlert> findByOriginAndDestinationAndStatus(
            String origin, String destination, AlertStatus status);
}
