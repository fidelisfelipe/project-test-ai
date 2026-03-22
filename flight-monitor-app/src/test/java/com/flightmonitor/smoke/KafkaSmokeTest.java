package com.flightmonitor.smoke;

import com.flightmonitor.config.KafkaConfig;
import com.flightmonitor.domain.enums.CabinClass;
import com.flightmonitor.messaging.dto.FlightSearchRequestMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("smoke")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.boot.admin.client.enabled=false"
})
@EmbeddedKafka(partitions = 1, topics = {
        KafkaConfig.FLIGHT_SEARCH_REQUESTS_TOPIC,
        KafkaConfig.FLIGHT_SEARCH_RESULTS_TOPIC,
        KafkaConfig.PRICE_ALERTS_TOPIC
})
class KafkaSmokeTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void producer_sendsMessage_topicReceivesIt() throws Exception {
        String correlationId = UUID.randomUUID().toString();
        FlightSearchRequestMessage message = new FlightSearchRequestMessage(
                correlationId, "BSB", "LIS", LocalDate.now().plusDays(5),
                null, 1, CabinClass.ECONOMY);

        long start = System.currentTimeMillis();
        kafkaTemplate.send(KafkaConfig.FLIGHT_SEARCH_REQUESTS_TOPIC, correlationId, message).get();
        long elapsed = System.currentTimeMillis() - start;

        assertThat(elapsed).isLessThan(5000);
    }
}
