package com.flightmonitor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the Amadeus API client credentials.
 */
@Configuration
public class AmadeusConfig {

    @Value("${amadeus.api.key}")
    private String key;

    @Value("${amadeus.api.secret}")
    private String secret;

    @Value("${amadeus.api.host}")
    private String host;

    public String getKey() {
        return key;
    }

    public String getSecret() {
        return secret;
    }

    public String getHost() {
        return host;
    }
}
