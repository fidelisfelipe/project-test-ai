package com.flightmonitor.messaging;

import com.flightmonitor.config.KafkaConfig;
import com.flightmonitor.messaging.dto.PriceAlertMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka producer for price alert messages.
 */
@Component
public class PriceAlertProducer {

    private static final Logger log = LoggerFactory.getLogger(PriceAlertProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PriceAlertProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Sends a price alert message to the Kafka topic.
     *
     * @param message the price alert message
     */
    public void sendAlert(PriceAlertMessage message) {
        String alertId = message.alertId().toString();
        MDC.put("correlationId", alertId);
        try {
            kafkaTemplate.send(KafkaConfig.PRICE_ALERTS_TOPIC, alertId, message)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to send price alert alertId={}: {}", alertId, ex.getMessage());
                        } else {
                            log.info("Sent price alert alertId={} to topic={}", alertId, KafkaConfig.PRICE_ALERTS_TOPIC);
                        }
                    });
        } finally {
            MDC.remove("correlationId");
        }
    }
}
