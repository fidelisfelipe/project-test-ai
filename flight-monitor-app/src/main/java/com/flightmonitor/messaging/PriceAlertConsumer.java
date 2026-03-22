package com.flightmonitor.messaging;

import com.flightmonitor.config.KafkaConfig;
import com.flightmonitor.messaging.dto.PriceAlertMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for price alert messages.
 */
@Component
@Profile("kafka")
public class PriceAlertConsumer {

    private static final Logger log = LoggerFactory.getLogger(PriceAlertConsumer.class);

    /**
     * Consumes a price alert message and handles notification delivery.
     *
     * @param record the Kafka consumer record
     * @param ack    the acknowledgment handle
     */
    @KafkaListener(
            topics = KafkaConfig.PRICE_ALERTS_TOPIC,
            groupId = "flight-monitor-alert-group",
            concurrency = "3"
    )
    public void consume(ConsumerRecord<String, PriceAlertMessage> record, Acknowledgment ack) {
        PriceAlertMessage message = record.value();
        String alertId = message.alertId().toString();
        MDC.put("correlationId", alertId);

        try {
            log.info("Processing price alert alertId={} user={} route={}->{} currentPrice={} targetPrice={}",
                    alertId, message.userEmail(), message.origin(), message.destination(),
                    message.currentPrice(), message.targetPrice());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing price alert alertId={}: {}", alertId, e.getMessage(), e);
            throw e;
        } finally {
            MDC.remove("correlationId");
        }
    }
}
