package com.flightmonitor.messaging.kafka;

import com.flightmonitor.config.KafkaConfig;
import com.flightmonitor.messaging.BrokerType;
import com.flightmonitor.messaging.MessageBus;
import com.flightmonitor.messaging.MessageSendResult;
import com.flightmonitor.messaging.dto.FlightSearchRequestMessage;
import com.flightmonitor.messaging.dto.PriceAlertMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka-based MessageBus implementation. Active when profile "kafka" is set.
 */
@Component
@Profile("kafka")
public class KafkaMessageBus implements MessageBus {

    private static final Logger log = LoggerFactory.getLogger(KafkaMessageBus.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaMessageBus(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public CompletableFuture<MessageSendResult> sendSearchRequest(FlightSearchRequestMessage message) {
        long start = System.currentTimeMillis();
        String correlationId = message.correlationId();
        Message<FlightSearchRequestMessage> kafkaMessage = MessageBuilder
                .withPayload(message)
                .setHeader(KafkaHeaders.TOPIC, KafkaConfig.FLIGHT_SEARCH_REQUESTS_TOPIC)
                .setHeader(KafkaHeaders.KEY, correlationId)
                .setHeader("correlationId", correlationId)
                .build();
        return kafkaTemplate.send(kafkaMessage)
                .handle((result, ex) -> {
                    long elapsed = System.currentTimeMillis() - start;
                    if (ex != null) {
                        log.error("[KAFKA] Failed to send search request correlationId={}: {}", correlationId, ex.getMessage());
                        return MessageSendResult.failed(BrokerType.KAFKA, ex.getMessage());
                    }
                    String offset = result != null ? String.valueOf(result.getRecordMetadata().offset()) : UUID.randomUUID().toString();
                    log.info("[KAFKA] Sent search request correlationId={} offset={}", correlationId, offset);
                    return MessageSendResult.ok(BrokerType.KAFKA, offset, elapsed);
                });
    }

    @Override
    public CompletableFuture<MessageSendResult> sendPriceAlert(PriceAlertMessage message) {
        long start = System.currentTimeMillis();
        String alertId = message.alertId().toString();
        return kafkaTemplate.send(KafkaConfig.PRICE_ALERTS_TOPIC, alertId, message)
                .handle((result, ex) -> {
                    long elapsed = System.currentTimeMillis() - start;
                    if (ex != null) {
                        log.error("[KAFKA] Failed to send price alert alertId={}: {}", alertId, ex.getMessage());
                        return MessageSendResult.failed(BrokerType.KAFKA, ex.getMessage());
                    }
                    String offset = result != null ? String.valueOf(result.getRecordMetadata().offset()) : UUID.randomUUID().toString();
                    log.info("[KAFKA] Sent price alert alertId={} offset={}", alertId, offset);
                    return MessageSendResult.ok(BrokerType.KAFKA, offset, elapsed);
                });
    }

    @Override
    public BrokerType getBrokerType() {
        return BrokerType.KAFKA;
    }

    @Override
    public boolean isAvailable() {
        try {
            kafkaTemplate.getProducerFactory().createProducer().partitionsFor("flight.search.requests");
            return true;
        } catch (Exception e) {
            log.warn("[KAFKA] Broker not available: {}", e.getMessage());
            return false;
        }
    }
}
