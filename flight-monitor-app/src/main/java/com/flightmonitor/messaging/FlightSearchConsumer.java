package com.flightmonitor.messaging;

import com.flightmonitor.client.AmadeusClient;
import com.flightmonitor.config.KafkaConfig;
import com.flightmonitor.dto.request.FlightSearchRequest;
import com.flightmonitor.dto.response.FlightOfferResponse;
import com.flightmonitor.messaging.dto.FlightSearchRequestMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Kafka consumer for flight search request messages.
 */
@Component
@Profile("kafka")
public class FlightSearchConsumer {

    private static final Logger log = LoggerFactory.getLogger(FlightSearchConsumer.class);

    private final AmadeusClient amadeusClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public FlightSearchConsumer(AmadeusClient amadeusClient, KafkaTemplate<String, Object> kafkaTemplate) {
        this.amadeusClient = amadeusClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Consumes a flight search request message, calls the Amadeus client, and publishes results.
     *
     * @param record the Kafka consumer record
     * @param ack    the acknowledgment handle
     */
    @KafkaListener(
            topics = KafkaConfig.FLIGHT_SEARCH_REQUESTS_TOPIC,
            groupId = "flight-monitor-group",
            concurrency = "10"
    )
    public void consume(ConsumerRecord<String, FlightSearchRequestMessage> record, Acknowledgment ack) {
        FlightSearchRequestMessage message = record.value();
        String correlationId = message.correlationId();
        MDC.put("correlationId", correlationId);

        try {
            log.info("Processing flight search request correlationId={} route={}->{}", correlationId, message.origin(), message.destination());

            FlightSearchRequest request = new FlightSearchRequest(
                    message.origin(),
                    message.destination(),
                    message.departureDate(),
                    message.returnDate(),
                    message.adults(),
                    message.cabinClass()
            );

            List<FlightOfferResponse> results = amadeusClient.searchFlights(request);
            kafkaTemplate.send(KafkaConfig.FLIGHT_SEARCH_RESULTS_TOPIC, correlationId, results);
            log.info("Published {} results to {} correlationId={}", results.size(), KafkaConfig.FLIGHT_SEARCH_RESULTS_TOPIC, correlationId);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing flight search request correlationId={}: {}", correlationId, e.getMessage(), e);
            throw e;
        } finally {
            MDC.remove("correlationId");
        }
    }
}
