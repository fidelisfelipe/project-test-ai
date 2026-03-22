package com.flightmonitor.client;

import com.flightmonitor.domain.enums.CabinClass;
import com.flightmonitor.dto.request.FlightSearchRequest;
import com.flightmonitor.dto.response.FlightOfferResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Mock implementation of AmadeusClient for sync and test profiles (free tier / CI environments).
 * Returns realistic simulated flight offers without calling the real Amadeus API.
 */
@Component
@Primary
@Profile({"sync", "test"})
public class AmadeusClientMock implements AmadeusClient {

    private static final Logger log = LoggerFactory.getLogger(AmadeusClientMock.class);

    private static final String[][] MOCK_AIRLINES = {
            {"LA", "LATAM Airlines", "1", "22h15m"},
            {"TP", "TAP Air Portugal", "1", "16h40m"},
            {"IB", "Iberia", "1", "18h20m"},
            {"AF", "Air France", "1", "17h50m"},
            {"KL", "KLM", "2", "24h10m"}
    };

    private static final double[] BASE_PRICES = {651.0, 742.0, 798.0, 831.0, 589.0};

    private final Random random = new Random();

    @Override
    public List<FlightOfferResponse> searchFlights(FlightSearchRequest request) {
        log.info("[MOCK] Returning simulated offers for {}->{}", request.origin(), request.destination());
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        List<FlightOfferResponse> offers = new ArrayList<>();
        for (int i = 0; i < MOCK_AIRLINES.length; i++) {
            String[] airline = MOCK_AIRLINES[i];
            double variation = 1.0 + (random.nextDouble() * 0.10 - 0.05);
            BigDecimal price = BigDecimal.valueOf(BASE_PRICES[i] * variation)
                    .setScale(2, RoundingMode.HALF_UP);

            CabinClass cabinClass = request.cabinClass() != null ? request.cabinClass() : CabinClass.ECONOMY;

            offers.add(new FlightOfferResponse(
                    UUID.randomUUID(),
                    request.origin(),
                    request.destination(),
                    request.departureDate(),
                    request.returnDate(),
                    airline[0],
                    price,
                    "EUR",
                    Integer.parseInt(airline[2]),
                    airline[3],
                    cabinClass,
                    null,
                    LocalDateTime.now(),
                    "MOCK"
            ));
        }

        offers.sort(Comparator.comparing(FlightOfferResponse::price));
        log.info("[MOCK] Returning {} simulated offers", offers.size());
        return offers;
    }
}
