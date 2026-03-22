package com.flightmonitor.service;

import com.flightmonitor.client.AmadeusClient;
import com.flightmonitor.domain.entity.FlightOffer;
import com.flightmonitor.domain.entity.SearchLog;
import com.flightmonitor.domain.enums.CabinClass;
import com.flightmonitor.dto.request.FlightSearchRequest;
import com.flightmonitor.dto.response.FlightOfferResponse;
import com.flightmonitor.exception.AmadeusApiException;
import com.flightmonitor.mapper.FlightOfferMapper;
import com.flightmonitor.messaging.MessageBus;
import com.flightmonitor.messaging.MessageSendResult;
import com.flightmonitor.messaging.dto.FlightSearchRequestMessage;
import com.flightmonitor.repository.FlightOfferRepository;
import com.flightmonitor.repository.SearchLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Implementation of the FlightSearchService.
 */
@Service
public class FlightSearchServiceImpl implements FlightSearchService {

    private static final Logger log = LoggerFactory.getLogger(FlightSearchServiceImpl.class);

    private final AmadeusClient amadeusClient;
    private final FlightOfferRepository flightOfferRepository;
    private final SearchLogRepository searchLogRepository;
    private final PriceHistoryService priceHistoryService;
    private final AlertService alertService;
    private final MessageBus messageBus;
    private final FlightOfferMapper flightOfferMapper;

    public FlightSearchServiceImpl(
            AmadeusClient amadeusClient,
            FlightOfferRepository flightOfferRepository,
            SearchLogRepository searchLogRepository,
            PriceHistoryService priceHistoryService,
            AlertService alertService,
            MessageBus messageBus,
            FlightOfferMapper flightOfferMapper) {
        this.amadeusClient = amadeusClient;
        this.flightOfferRepository = flightOfferRepository;
        this.searchLogRepository = searchLogRepository;
        this.priceHistoryService = priceHistoryService;
        this.alertService = alertService;
        this.messageBus = messageBus;
        this.flightOfferMapper = flightOfferMapper;
    }

    /**
     * Searches for available flight offers. Validates inputs, checks cache, calls the Amadeus client,
     * persists results, and checks price alerts.
     *
     * @param request the search parameters
     * @return list of flight offers sorted by price ascending
     */
    @Override
    @Cacheable(value = "flightSearches", key = "#request.origin() + '-' + #request.destination() + '-' + #request.departureDate()")
    @Transactional
    public List<FlightOfferResponse> searchFlights(FlightSearchRequest request) {
        validateRequest(request);
        long startTime = System.currentTimeMillis();
        String correlationId = UUID.randomUUID().toString();
        log.info("Starting flight search correlationId={} route={}->{} date={}", correlationId, request.origin(), request.destination(), request.departureDate());

        try {
            MessageSendResult sendResult = messageBus.sendSearchRequest(new FlightSearchRequestMessage(
                    correlationId,
                    request.origin(),
                    request.destination(),
                    request.departureDate(),
                    request.returnDate(),
                    request.adults(),
                    request.cabinClass()
            )).get(30, TimeUnit.SECONDS);
            if (!sendResult.success()) {
                throw new AmadeusApiException("Failed to dispatch search request: " + sendResult.errorMessage(), null);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AmadeusApiException("Search request interrupted", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new AmadeusApiException("Search request dispatch failed: " + e.getMessage(), e);
        }

        List<FlightOfferResponse> offers = amadeusClient.searchFlights(request);
        List<FlightOfferResponse> sorted = offers.stream()
                .sorted(Comparator.comparing(FlightOfferResponse::price))
                .toList();

        persistOffers(sorted, request);
        priceHistoryService.saveSnapshot(request.origin(), request.destination(), request.departureDate(), sorted);

        if (!sorted.isEmpty()) {
            alertService.checkAlertsForRoute(request.origin(), request.destination(), sorted.get(0).price());
        }

        long durationMs = System.currentTimeMillis() - startTime;
        recordSearchLog(request, sorted.size(), durationMs, "API", null);
        log.info("Flight search completed correlationId={} results={} durationMs={}", correlationId, sorted.size(), durationMs);
        return sorted;
    }

    private void validateRequest(FlightSearchRequest request) {
        if (request.origin() == null || !request.origin().matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Origin must be a valid 3-letter IATA code");
        }
        if (request.destination() == null || !request.destination().matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Destination must be a valid 3-letter IATA code");
        }
        if (request.departureDate() == null || !request.departureDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Departure date must be in the future");
        }
    }

    @Transactional
    protected void persistOffers(List<FlightOfferResponse> offers, FlightSearchRequest request) {
        for (FlightOfferResponse resp : offers) {
            FlightOffer entity = new FlightOffer();
            entity.setOrigin(resp.origin());
            entity.setDestination(resp.destination());
            entity.setDepartureDate(resp.departureDate());
            entity.setReturnDate(resp.returnDate());
            entity.setAirline(resp.airline());
            entity.setPrice(resp.price());
            entity.setCurrency(resp.currency());
            entity.setStops(resp.stops());
            entity.setDuration(resp.duration());
            entity.setCabinClass(resp.cabinClass() != null ? resp.cabinClass() : CabinClass.ECONOMY);
            entity.setDeepLink(resp.deepLink());
            entity.setSource(resp.source() != null ? resp.source() : "AMADEUS");
            flightOfferRepository.save(entity);
        }
    }

    @Transactional
    protected void recordSearchLog(FlightSearchRequest request, int resultsCount, long durationMs, String source, String errorMessage) {
        SearchLog log = new SearchLog();
        log.setOrigin(request.origin());
        log.setDestination(request.destination());
        log.setDepartureDate(request.departureDate());
        log.setExecutedAt(LocalDateTime.now());
        log.setResultsCount(resultsCount);
        log.setDurationMs(durationMs);
        log.setSource(source);
        log.setErrorMessage(errorMessage);
        searchLogRepository.save(log);
    }
}
