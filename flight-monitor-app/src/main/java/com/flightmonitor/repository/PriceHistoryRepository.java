package com.flightmonitor.repository;

import com.flightmonitor.domain.entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for PriceHistory entities.
 */
@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, UUID> {

    List<PriceHistory> findByOriginAndDestinationAndDepartureDateOrderByRecordedAtDesc(
            String origin, String destination, LocalDate departureDate);

    List<PriceHistory> findByOriginAndDestinationAndRecordedAtAfterOrderByRecordedAtDesc(
            String origin, String destination, LocalDateTime after);
}
