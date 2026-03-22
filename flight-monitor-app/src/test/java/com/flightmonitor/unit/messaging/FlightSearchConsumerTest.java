package com.flightmonitor.unit.messaging;

import com.flightmonitor.client.AmadeusClient;
import com.flightmonitor.config.KafkaConfig;
import com.flightmonitor.domain.enums.CabinClass;
import com.flightmonitor.dto.response.FlightOfferResponse;
import com.flightmonitor.messaging.FlightSearchConsumer;
import com.flightmonitor.messaging.dto.FlightSearchRequestMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlightSearchConsumerTest {

    @Mock
    private AmadeusClient amadeusClient;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private Acknowledgment acknowledgment;

    private FlightSearchConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new FlightSearchConsumer(amadeusClient, kafkaTemplate);
    }

    @Test
    void consume_callsAmadeusClientWithCorrectParams() {
        FlightSearchRequestMessage message = searchMessage();
        ConsumerRecord<String, FlightSearchRequestMessage> record = buildRecord(message);
        when(amadeusClient.searchFlights(any())).thenReturn(List.of());
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));
        doNothing().when(acknowledgment).acknowledge();

        consumer.consume(record, acknowledgment);

        verify(amadeusClient).searchFlights(any());
    }

    @Test
    void consume_publishesResultToResultsTopic() {
        FlightSearchRequestMessage message = searchMessage();
        ConsumerRecord<String, FlightSearchRequestMessage> record = buildRecord(message);
        FlightOfferResponse offer = new FlightOfferResponse(UUID.randomUUID(), "BSB", "LIS",
                LocalDate.now().plusDays(5), null, "TP", new BigDecimal("299"), "EUR", 0, "PT5H", CabinClass.ECONOMY, null, LocalDateTime.now(), "AMADEUS");
        when(amadeusClient.searchFlights(any())).thenReturn(List.of(offer));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));
        doNothing().when(acknowledgment).acknowledge();

        consumer.consume(record, acknowledgment);

        verify(kafkaTemplate).send(eq(KafkaConfig.FLIGHT_SEARCH_RESULTS_TOPIC), anyString(), any());
    }

    @Test
    void consume_doesManualAckAfterSuccess() {
        FlightSearchRequestMessage message = searchMessage();
        ConsumerRecord<String, FlightSearchRequestMessage> record = buildRecord(message);
        when(amadeusClient.searchFlights(any())).thenReturn(List.of());
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));
        doNothing().when(acknowledgment).acknowledge();

        consumer.consume(record, acknowledgment);

        verify(acknowledgment).acknowledge();
    }

    @Test
    void consume_throwsWhenAmadeusClientFails() {
        FlightSearchRequestMessage message = searchMessage();
        ConsumerRecord<String, FlightSearchRequestMessage> record = buildRecord(message);
        when(amadeusClient.searchFlights(any())).thenThrow(new RuntimeException("Amadeus down"));

        assertThatThrownBy(() -> consumer.consume(record, acknowledgment))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Amadeus down");
    }

    private FlightSearchRequestMessage searchMessage() {
        return new FlightSearchRequestMessage(UUID.randomUUID().toString(), "BSB", "LIS",
                LocalDate.now().plusDays(5), null, 1, CabinClass.ECONOMY);
    }

    private ConsumerRecord<String, FlightSearchRequestMessage> buildRecord(FlightSearchRequestMessage message) {
        return new ConsumerRecord<>(KafkaConfig.FLIGHT_SEARCH_REQUESTS_TOPIC, 0, 0L, message.correlationId(), message);
    }
}
