package com.flightmonitor.client;

import com.flightmonitor.config.AggregatorConfig;
import com.flightmonitor.dto.request.FlightSearchRequest;
import com.flightmonitor.dto.response.FlightOfferResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Aggregator flight client that orchestrates parallel calls to multiple flight sources,
 * combines results, deduplicates by flight number, and returns sorted by price.
 *
 * <p>Activated when {@code flight.aggregator.enabled=true}.</p>
 */
@Component
@Primary
@ConditionalOnProperty(prefix = "flight.aggregator", name = "enabled", havingValue = "true")
public class AggregatorFlightClient implements AmadeusClient {

    private static final Logger log = LoggerFactory.getLogger(AggregatorFlightClient.class);

    private final AggregatorConfig config;
    private final Map<String, AmadeusClient> availableClients;

    public AggregatorFlightClient(
            AggregatorConfig config,
            ObjectProvider<AmadeusClientImpl> amadeusClient,
            ObjectProvider<SerpApiFlightClient> serpApiClient,
            ObjectProvider<KiwiTequilaClient> kiwiClient,
            ObjectProvider<AmadeusClientMock> mockClient
    ) {
        this.config = config;
        this.availableClients = new LinkedHashMap<>();
        amadeusClient.ifAvailable(c -> availableClients.put("amadeus", c));
        serpApiClient.ifAvailable(c -> availableClients.put("serpapi", c));
        kiwiClient.ifAvailable(c -> availableClients.put("kiwi", c));
        mockClient.ifAvailable(c -> availableClients.put("mock", c));
    }

    @Override
    public FlightSource getSource() {
        return FlightSource.AGGREGATOR;
    }

    @Override
    public List<FlightOfferResponse> searchFlights(FlightSearchRequest request) {
        List<AmadeusClient> activeClients = config.getSources().stream()
                .filter(availableClients::containsKey)
                .map(availableClients::get)
                .toList();

        if (activeClients.isEmpty()) {
            log.warn("[Aggregator] No active clients found for configured sources: {}", config.getSources());
            return List.of();
        }

        List<CompletableFuture<List<FlightOfferResponse>>> futures = activeClients.stream()
                .map(client -> CompletableFuture
                        .supplyAsync(() -> safeSearch(client, request))
                        .orTimeout(config.getTimeoutSeconds(), TimeUnit.SECONDS)
                        .exceptionally(ex -> {
                            log.warn("[Aggregator] Source {} failed: {}",
                                    client.getClass().getSimpleName(), ex.getMessage());
                            return List.of();
                        }))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<FlightOfferResponse> allResults = futures.stream()
                .flatMap(f -> f.join().stream())
                .collect(Collectors.toList());

        List<FlightOfferResponse> finalResults;
        if (config.isDeduplicateByFlightNumber()) {
            Map<String, FlightOfferResponse> deduplicated = new LinkedHashMap<>();
            for (FlightOfferResponse offer : allResults) {
                String key = buildDeduplicationKey(offer);
                deduplicated.merge(key, offer,
                        (existing, incoming) -> existing.price().compareTo(incoming.price()) <= 0
                                ? existing : incoming);
            }

            log.info("[Aggregator] Completed: {} total → {} after dedup, cheapest: {} {}",
                    allResults.size(),
                    deduplicated.size(),
                    deduplicated.values().stream().mapToDouble(o -> o.price().doubleValue()).min().orElse(0),
                    deduplicated.values().stream().findFirst().map(FlightOfferResponse::currency).orElse(""));

            finalResults = deduplicated.values().stream()
                    .sorted(Comparator.comparing(FlightOfferResponse::price))
                    .collect(Collectors.toList());
        } else {
            finalResults = allResults.stream()
                    .sorted(Comparator.comparing(FlightOfferResponse::price))
                    .collect(Collectors.toList());
        }

        return finalResults;
    }

    private List<FlightOfferResponse> safeSearch(AmadeusClient client, FlightSearchRequest req) {
        try {
            log.info("[Aggregator] Querying source: {}", client.getClass().getSimpleName());
            List<FlightOfferResponse> results = client.searchFlights(req);
            log.info("[Aggregator] Source {} returned {} results",
                    client.getClass().getSimpleName(), results.size());
            return results;
        } catch (Exception e) {
            log.warn("[Aggregator] Source {} threw: {}",
                    client.getClass().getSimpleName(), e.getMessage());
            return List.of();
        }
    }

    private String buildDeduplicationKey(FlightOfferResponse offer) {
        String flightRef = offer.airline() != null && !offer.airline().isBlank()
                ? offer.airline()
                : offer.origin() + offer.destination();
        return flightRef + "|" + offer.departureDate() + "|" + offer.origin() + "|" + offer.destination();
    }
}
