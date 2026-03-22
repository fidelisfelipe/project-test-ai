package com.flightmonitor.messaging.sync;

import com.flightmonitor.messaging.BrokerType;
import com.flightmonitor.messaging.MessageBus;
import com.flightmonitor.messaging.MessageHandler;
import com.flightmonitor.messaging.MessageSendResult;
import com.flightmonitor.messaging.dto.FlightSearchRequestMessage;
import com.flightmonitor.messaging.dto.PriceAlertMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Synchronous MessageBus implementation for environments without a broker (free tier, CI).
 * Processes messages immediately in the same thread.
 */
@Component
@Profile("sync")
public class SyncMessageBus implements MessageBus {

    private static final Logger log = LoggerFactory.getLogger(SyncMessageBus.class);

    private final MessageHandler messageHandler;

    public SyncMessageBus(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    @Override
    public CompletableFuture<MessageSendResult> sendSearchRequest(FlightSearchRequestMessage message) {
        long start = System.currentTimeMillis();
        MDC.put("correlationId", message.correlationId());
        try {
            log.info("[SYNC][{}] Processing search {}->{} directly",
                    message.correlationId(), message.origin(), message.destination());
            var result = messageHandler.handleSearchRequest(message);
            long elapsed = System.currentTimeMillis() - start;
            log.info("[SYNC][{}] Completed in {}ms — {} offers",
                    message.correlationId(), elapsed, result.offers().size());
            return CompletableFuture.completedFuture(
                    MessageSendResult.ok(BrokerType.SYNC, message.correlationId(), elapsed));
        } catch (Exception ex) {
            log.error("[SYNC][{}] Error: {}", message.correlationId(), ex.getMessage());
            return CompletableFuture.completedFuture(
                    MessageSendResult.failed(BrokerType.SYNC, ex.getMessage()));
        } finally {
            MDC.remove("correlationId");
        }
    }

    @Override
    public CompletableFuture<MessageSendResult> sendPriceAlert(PriceAlertMessage message) {
        long start = System.currentTimeMillis();
        try {
            messageHandler.handlePriceAlert(message);
            long elapsed = System.currentTimeMillis() - start;
            return CompletableFuture.completedFuture(
                    MessageSendResult.ok(BrokerType.SYNC, message.alertId().toString(), elapsed));
        } catch (Exception ex) {
            log.warn("[SYNC] Price alert handling failed alertId={}: {}", message.alertId(), ex.getMessage());
            return CompletableFuture.completedFuture(
                    MessageSendResult.failed(BrokerType.SYNC, ex.getMessage()));
        }
    }

    @Override
    public BrokerType getBrokerType() {
        return BrokerType.SYNC;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
