package com.flightmonitor.service;

import com.flightmonitor.domain.entity.PriceHistory;
import com.flightmonitor.dto.response.FlightOfferResponse;
import com.flightmonitor.dto.response.PriceHistoryResponse;
import com.flightmonitor.repository.PriceHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Service implementation for price history operations.
 */
@Service
public class PriceHistoryServiceImpl implements PriceHistoryService {

    private static final Logger log = LoggerFactory.getLogger(PriceHistoryServiceImpl.class);

    private final PriceHistoryRepository priceHistoryRepository;

    public PriceHistoryServiceImpl(PriceHistoryRepository priceHistoryRepository) {
        this.priceHistoryRepository = priceHistoryRepository;
    }

    /**
     * Retrieves price history for a route over the last N days, sorted by recordedAt descending.
     *
     * @param origin      the origin IATA code
     * @param destination the destination IATA code
     * @param days        number of days to look back
     * @return sorted list of price history entries
     */
    @Override
    public List<PriceHistoryResponse> getHistory(String origin, String destination, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<PriceHistory> records = priceHistoryRepository
                .findByOriginAndDestinationAndRecordedAtAfterOrderByRecordedAtDesc(origin, destination, since);

        List<PriceHistoryResponse> responses = records.stream()
                .map(this::toResponse)
                .sorted(Comparator.comparing(PriceHistoryResponse::recordedAt).reversed())
                .toList();

        log.info("Retrieved {} history records for route {}->{} over {} days", responses.size(), origin, destination, days);
        return responses;
    }

    /**
     * Saves a price snapshot for a given route from a list of offers.
     *
     * @param origin        the origin IATA code
     * @param destination   the destination IATA code
     * @param departureDate the departure date
     * @param offers        list of offer responses to compute min/max/avg from
     */
    @Override
    @Transactional
    public void saveSnapshot(String origin, String destination, LocalDate departureDate, List<FlightOfferResponse> offers) {
        if (offers.isEmpty()) {
            return;
        }
        BigDecimal minPrice = offers.stream().map(FlightOfferResponse::price).min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        BigDecimal maxPrice = offers.stream().map(FlightOfferResponse::price).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        BigDecimal sum = offers.stream().map(FlightOfferResponse::price).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgPrice = sum.divide(BigDecimal.valueOf(offers.size()), 2, RoundingMode.HALF_UP);

        List<PriceHistoryResponse> history = priceHistoryRepository
                .findByOriginAndDestinationAndDepartureDateOrderByRecordedAtDesc(origin, destination, departureDate)
                .stream().map(this::toResponse).toList();

        String trend = calculateTrend(history);

        PriceHistory snapshot = new PriceHistory();
        snapshot.setOrigin(origin);
        snapshot.setDestination(destination);
        snapshot.setDepartureDate(departureDate);
        snapshot.setMinPrice(minPrice);
        snapshot.setMaxPrice(maxPrice);
        snapshot.setAvgPrice(avgPrice);
        snapshot.setOffersCount(offers.size());
        snapshot.setRecordedAt(LocalDateTime.now());
        snapshot.setTrend(trend);

        priceHistoryRepository.save(snapshot);
        log.info("Saved price snapshot for route {}->{} date={} min={} max={} avg={}", origin, destination, departureDate, minPrice, maxPrice, avgPrice);
    }

    /**
     * Calculates price trend from a list of history entries (most recent first).
     *
     * @param history ordered price history records
     * @return "UP", "DOWN", or "STABLE"
     */
    @Override
    public String calculateTrend(List<PriceHistoryResponse> history) {
        if (history.size() < 2) {
            return "STABLE";
        }
        BigDecimal latest = history.get(0).avgPrice();
        BigDecimal previous = history.get(1).avgPrice();
        int comparison = latest.compareTo(previous);
        if (comparison > 0) {
            return "UP";
        } else if (comparison < 0) {
            return "DOWN";
        }
        return "STABLE";
    }

    private PriceHistoryResponse toResponse(PriceHistory ph) {
        return new PriceHistoryResponse(
                ph.getId(),
                ph.getOrigin(),
                ph.getDestination(),
                ph.getDepartureDate(),
                ph.getMinPrice(),
                ph.getMaxPrice(),
                ph.getAvgPrice(),
                ph.getOffersCount(),
                ph.getRecordedAt(),
                ph.getTrend()
        );
    }
}
