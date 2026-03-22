package com.flightmonitor.messaging.rabbitmq;

import com.flightmonitor.messaging.BrokerType;
import com.flightmonitor.messaging.MessageBus;
import com.flightmonitor.messaging.MessageSendResult;
import com.flightmonitor.messaging.dto.FlightSearchRequestMessage;
import com.flightmonitor.messaging.dto.PriceAlertMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * RabbitMQ-based MessageBus implementation. Active when profile "rabbitmq" is set.
 */
@Component
@Profile("rabbitmq")
public class RabbitMQMessageBus implements MessageBus {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQMessageBus.class);

    static final String FLIGHT_EXCHANGE = "flight.exchange";
    static final String SEARCH_REQUESTS_ROUTING_KEY = "flight.search.requests";
    static final String PRICE_ALERTS_ROUTING_KEY = "price.alerts";

    private final RabbitTemplate rabbitTemplate;

    public RabbitMQMessageBus(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public CompletableFuture<MessageSendResult> sendSearchRequest(FlightSearchRequestMessage message) {
        long start = System.currentTimeMillis();
        String messageId = UUID.randomUUID().toString();
        try {
            rabbitTemplate.convertAndSend(FLIGHT_EXCHANGE, SEARCH_REQUESTS_ROUTING_KEY, message);
            long elapsed = System.currentTimeMillis() - start;
            log.info("[RABBITMQ] Sent search request correlationId={}", message.correlationId());
            return CompletableFuture.completedFuture(
                    MessageSendResult.ok(BrokerType.RABBITMQ, messageId, elapsed));
        } catch (AmqpException ex) {
            log.error("[RABBITMQ] Failed to send search request correlationId={}: {}", message.correlationId(), ex.getMessage());
            return CompletableFuture.completedFuture(
                    MessageSendResult.failed(BrokerType.RABBITMQ, ex.getMessage()));
        }
    }

    @Override
    public CompletableFuture<MessageSendResult> sendPriceAlert(PriceAlertMessage message) {
        long start = System.currentTimeMillis();
        String messageId = UUID.randomUUID().toString();
        try {
            rabbitTemplate.convertAndSend(FLIGHT_EXCHANGE, PRICE_ALERTS_ROUTING_KEY, message);
            long elapsed = System.currentTimeMillis() - start;
            log.info("[RABBITMQ] Sent price alert alertId={}", message.alertId());
            return CompletableFuture.completedFuture(
                    MessageSendResult.ok(BrokerType.RABBITMQ, messageId, elapsed));
        } catch (AmqpException ex) {
            log.error("[RABBITMQ] Failed to send price alert alertId={}: {}", message.alertId(), ex.getMessage());
            return CompletableFuture.completedFuture(
                    MessageSendResult.failed(BrokerType.RABBITMQ, ex.getMessage()));
        }
    }

    @Override
    public BrokerType getBrokerType() {
        return BrokerType.RABBITMQ;
    }

    @Override
    public boolean isAvailable() {
        try {
            return Boolean.TRUE.equals(rabbitTemplate.execute(channel -> channel.isOpen()));
        } catch (AmqpException ex) {
            log.warn("[RABBITMQ] Broker not available: {}", ex.getMessage());
            return false;
        }
    }
}
