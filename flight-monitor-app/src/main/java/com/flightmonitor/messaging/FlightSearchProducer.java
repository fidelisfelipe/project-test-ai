package com.flightmonitor.messaging;

import com.flightmonitor.config.KafkaConfig;
import com.flightmonitor.messaging.dto.FlightSearchRequestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * Kafka producer for flight search request messages.
 */
@Component
public class FlightSearchProducer {

    private static final Logger log = LoggerFactory.getLogger(FlightSearchProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public FlightSearchProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Sends a flight search request message to the Kafka topic.
     *
     * @param message the flight search request message
     */
    public void sendSearchRequest(FlightSearchRequestMessage message) {
        String correlationId = message.correlationId();
        MDC.put("correlationId", correlationId);
        try {
            Message<FlightSearchRequestMessage> kafkaMessage = MessageBuilder
                    .withPayload(message)
                    .setHeader(KafkaHeaders.TOPIC, KafkaConfig.FLIGHT_SEARCH_REQUESTS_TOPIC)
                    .setHeader(KafkaHeaders.KEY, correlationId)
                    .setHeader("correlationId", correlationId)
                    .build();
            kafkaTemplate.send(kafkaMessage)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to send flight search request correlationId={}: {}", correlationId, ex.getMessage());
                        } else {
                            log.info("Sent flight search request correlationId={} to topic={}", correlationId, KafkaConfig.FLIGHT_SEARCH_REQUESTS_TOPIC);
                        }
                    });
        } finally {
            MDC.remove("correlationId");
        }
    }
}
