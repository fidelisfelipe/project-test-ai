package com.flightmonitor.messaging.pulsar;

import com.flightmonitor.messaging.BrokerType;
import com.flightmonitor.messaging.MessageBus;
import com.flightmonitor.messaging.MessageSendResult;
import com.flightmonitor.messaging.dto.FlightSearchRequestMessage;
import com.flightmonitor.messaging.dto.PriceAlertMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Apache Pulsar-based MessageBus implementation. Active when profile "pulsar" is set.
 */
@Component
@Profile("pulsar")
public class PulsarMessageBus implements MessageBus {

    private static final Logger log = LoggerFactory.getLogger(PulsarMessageBus.class);

    static final String SEARCH_REQUESTS_TOPIC = "persistent://public/default/flight-search-requests";
    static final String PRICE_ALERTS_TOPIC = "persistent://public/default/price-alerts";

    private final PulsarTemplate<FlightSearchRequestMessage> searchRequestTemplate;
    private final PulsarTemplate<PriceAlertMessage> priceAlertTemplate;

    public PulsarMessageBus(PulsarTemplate<FlightSearchRequestMessage> searchRequestTemplate,
                            PulsarTemplate<PriceAlertMessage> priceAlertTemplate) {
        this.searchRequestTemplate = searchRequestTemplate;
        this.priceAlertTemplate = priceAlertTemplate;
    }

    @Override
    public CompletableFuture<MessageSendResult> sendSearchRequest(FlightSearchRequestMessage message) {
        long start = System.currentTimeMillis();
        String messageId = UUID.randomUUID().toString();
        try {
            searchRequestTemplate.send(SEARCH_REQUESTS_TOPIC, message);
            long elapsed = System.currentTimeMillis() - start;
            log.info("[PULSAR] Sent search request correlationId={}", message.correlationId());
            return CompletableFuture.completedFuture(
                    MessageSendResult.ok(BrokerType.PULSAR, messageId, elapsed));
        } catch (Exception ex) {
            log.error("[PULSAR] Failed to send search request correlationId={}: {}", message.correlationId(), ex.getMessage());
            return CompletableFuture.completedFuture(
                    MessageSendResult.failed(BrokerType.PULSAR, ex.getMessage()));
        }
    }

    @Override
    public CompletableFuture<MessageSendResult> sendPriceAlert(PriceAlertMessage message) {
        long start = System.currentTimeMillis();
        String messageId = UUID.randomUUID().toString();
        try {
            priceAlertTemplate.send(PRICE_ALERTS_TOPIC, message);
            long elapsed = System.currentTimeMillis() - start;
            log.info("[PULSAR] Sent price alert alertId={}", message.alertId());
            return CompletableFuture.completedFuture(
                    MessageSendResult.ok(BrokerType.PULSAR, messageId, elapsed));
        } catch (Exception ex) {
            log.error("[PULSAR] Failed to send price alert alertId={}: {}", message.alertId(), ex.getMessage());
            return CompletableFuture.completedFuture(
                    MessageSendResult.failed(BrokerType.PULSAR, ex.getMessage()));
        }
    }

    @Override
    public BrokerType getBrokerType() {
        return BrokerType.PULSAR;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
