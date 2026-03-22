package com.flightmonitor.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * RabbitMQ configuration. Active only when profile "rabbitmq" is set.
 */
@Configuration
@Profile("rabbitmq")
public class RabbitMQConfig {

    static final String FLIGHT_EXCHANGE = "flight.exchange";
    static final String SEARCH_REQUESTS_QUEUE = "flight.search.requests";
    static final String PRICE_ALERTS_QUEUE = "price.alerts";

    @Bean
    public TopicExchange flightExchange() {
        return new TopicExchange(FLIGHT_EXCHANGE, true, false);
    }

    @Bean
    public Queue searchRequestsQueue() {
        return new Queue(SEARCH_REQUESTS_QUEUE, true);
    }

    @Bean
    public Queue priceAlertsQueue() {
        return new Queue(PRICE_ALERTS_QUEUE, true);
    }

    @Bean
    public Binding searchRequestsBinding(Queue searchRequestsQueue, TopicExchange flightExchange) {
        return BindingBuilder.bind(searchRequestsQueue).to(flightExchange).with(SEARCH_REQUESTS_QUEUE);
    }

    @Bean
    public Binding priceAlertsBinding(Queue priceAlertsQueue, TopicExchange flightExchange) {
        return BindingBuilder.bind(priceAlertsQueue).to(flightExchange).with(PRICE_ALERTS_QUEUE);
    }

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
