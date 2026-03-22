package com.flightmonitor.config;

import com.flightmonitor.messaging.MessageBus;
import com.flightmonitor.messaging.MessageHandler;
import com.flightmonitor.messaging.sync.SyncMessageBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the messaging abstraction layer.
 * Provides a fallback MessageBus if no profile-specific one is defined.
 */
@Configuration
public class MessagingConfig {

    private static final Logger log = LoggerFactory.getLogger(MessagingConfig.class);

    @Bean
    @ConditionalOnMissingBean(MessageBus.class)
    public MessageBus fallbackMessageBus(MessageHandler messageHandler) {
        log.warn("No MessageBus profile active — falling back to SYNC mode");
        return new SyncMessageBus(messageHandler);
    }

    @Bean
    public HealthIndicator messageBusHealthIndicator(MessageBus messageBus) {
        return () -> {
            boolean available = messageBus.isAvailable();
            Health.Builder builder = available ? Health.up() : Health.down();
            return builder
                    .withDetail("brokerType", messageBus.getBrokerType().name())
                    .withDetail("available", available)
                    .build();
        };
    }
}
