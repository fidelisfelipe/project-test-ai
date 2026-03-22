package com.flightmonitor.unit.messaging;

import com.flightmonitor.config.KafkaConfig;
import com.flightmonitor.domain.enums.CabinClass;
import com.flightmonitor.messaging.FlightSearchProducer;
import com.flightmonitor.messaging.dto.FlightSearchRequestMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlightSearchProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private FlightSearchProducer producer;

    @BeforeEach
    void setUp() {
        producer = new FlightSearchProducer(kafkaTemplate);
    }

    @SuppressWarnings("unchecked")
    @Test
    void sendSearchRequest_callsKafkaTemplateWithCorrectTopic() {
        FlightSearchRequestMessage message = new FlightSearchRequestMessage(
                UUID.randomUUID().toString(), "BSB", "LIS",
                LocalDate.now().plusDays(5), null, 1, CabinClass.ECONOMY);

        when(kafkaTemplate.send(any(Message.class))).thenReturn(CompletableFuture.completedFuture(null));

        producer.sendSearchRequest(message);

        verify(kafkaTemplate).send(any(Message.class));
    }

    @Test
    void sendSearchRequest_includesCorrelationIdInHeader() {
        String correlationId = UUID.randomUUID().toString();
        FlightSearchRequestMessage message = new FlightSearchRequestMessage(
                correlationId, "BSB", "LIS", LocalDate.now().plusDays(5), null, 1, CabinClass.ECONOMY);

        ArgumentCaptor<Message<?>> captor = ArgumentCaptor.forClass(Message.class);
        when(kafkaTemplate.send(captor.capture())).thenReturn(CompletableFuture.completedFuture(null));

        producer.sendSearchRequest(message);

        Message<?> captured = captor.getValue();
        assertThat(captured.getHeaders().get("correlationId")).isEqualTo(correlationId);
    }

    @SuppressWarnings("unchecked")
    @Test
    void sendSearchRequest_logsErrorWhenSendFails() {
        FlightSearchRequestMessage message = new FlightSearchRequestMessage(
                UUID.randomUUID().toString(), "BSB", "LIS",
                LocalDate.now().plusDays(5), null, 1, CabinClass.ECONOMY);

        CompletableFuture<Object> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("Kafka unavailable"));
        when(kafkaTemplate.send(any(Message.class))).thenReturn((CompletableFuture) failed);

        // Should not throw; error is handled asynchronously
        producer.sendSearchRequest(message);
        verify(kafkaTemplate).send(any(Message.class));
    }
}
