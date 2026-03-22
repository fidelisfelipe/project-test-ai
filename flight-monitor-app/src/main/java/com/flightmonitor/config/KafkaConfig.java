package com.flightmonitor.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Kafka configuration including topics and error handling.
 */
@Configuration
public class KafkaConfig {

    public static final String FLIGHT_SEARCH_REQUESTS_TOPIC = "flight.search.requests";
    public static final String FLIGHT_SEARCH_RESULTS_TOPIC = "flight.search.results";
    public static final String PRICE_ALERTS_TOPIC = "price.alerts";

    @Bean
    public NewTopic flightSearchRequestsTopic() {
        return TopicBuilder.name(FLIGHT_SEARCH_REQUESTS_TOPIC)
                .partitions(6)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, "86400000")
                .build();
    }

    @Bean
    public NewTopic flightSearchResultsTopic() {
        return TopicBuilder.name(FLIGHT_SEARCH_RESULTS_TOPIC)
                .partitions(6)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, "86400000")
                .build();
    }

    @Bean
    public NewTopic priceAlertsTopic() {
        return TopicBuilder.name(PRICE_ALERTS_TOPIC)
                .partitions(6)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, "86400000")
                .build();
    }

    @Bean
    public NewTopic flightSearchRequestsDlt() {
        return TopicBuilder.name(FLIGHT_SEARCH_REQUESTS_TOPIC + ".DLT")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic flightSearchResultsDlt() {
        return TopicBuilder.name(FLIGHT_SEARCH_RESULTS_TOPIC + ".DLT")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic priceAlertsDlt() {
        return TopicBuilder.name(PRICE_ALERTS_TOPIC + ".DLT")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public CommonErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxAttempts(3);
        return new DefaultErrorHandler(recoverer, backOff);
    }

    @Bean(name = "virtualThreadExecutor")
    public Executor virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
