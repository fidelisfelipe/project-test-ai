package com.flightmonitor.repository;

import com.flightmonitor.domain.entity.FlightOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Repository for FlightOffer entities.
 */
@Repository
public interface FlightOfferRepository extends JpaRepository<FlightOffer, UUID> {

    List<FlightOffer> findByOriginAndDestinationAndDepartureDate(
            String origin, String destination, LocalDate departureDate);

    List<FlightOffer> findByOriginAndDestinationOrderByPriceAsc(
            String origin, String destination);
}
