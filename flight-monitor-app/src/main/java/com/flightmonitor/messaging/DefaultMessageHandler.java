package com.flightmonitor.messaging;

import com.flightmonitor.client.AmadeusClient;
import com.flightmonitor.dto.request.FlightSearchRequest;
import com.flightmonitor.dto.response.FlightOfferResponse;
import com.flightmonitor.messaging.dto.FlightSearchRequestMessage;
import com.flightmonitor.messaging.dto.FlightSearchResultMessage;
import com.flightmonitor.messaging.dto.PriceAlertMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Default implementation of MessageHandler that delegates to AmadeusClient.
 */
@Component
public class DefaultMessageHandler implements MessageHandler {

    private static final Logger log = LoggerFactory.getLogger(DefaultMessageHandler.class);

    private final AmadeusClient amadeusClient;

    public DefaultMessageHandler(AmadeusClient amadeusClient) {
        this.amadeusClient = amadeusClient;
    }

    @Override
    public FlightSearchResultMessage handleSearchRequest(FlightSearchRequestMessage message) throws Exception {
        FlightSearchRequest request = new FlightSearchRequest(
                message.origin(),
                message.destination(),
                message.departureDate(),
                message.returnDate(),
                message.adults(),
                message.cabinClass()
        );
        List<FlightOfferResponse> offers = amadeusClient.searchFlights(request);
        return new FlightSearchResultMessage(message.correlationId(), offers);
    }

    @Override
    public void handlePriceAlert(PriceAlertMessage message) throws Exception {
        log.info("[HANDLER] Price alert triggered alertId={} user={} route={}->{} currentPrice={} targetPrice={}",
                message.alertId(), message.userEmail(), message.origin(), message.destination(),
                message.currentPrice(), message.targetPrice());
    }
}
