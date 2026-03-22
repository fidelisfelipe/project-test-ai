package com.flightmonitor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Apache Pulsar configuration. Active only when profile "pulsar" is set.
 * Spring Boot auto-configures Pulsar via spring-boot-starter-pulsar.
 * Additional beans can be added here as needed.
 */
@Configuration
@Profile("pulsar")
public class PulsarConfig {
}
