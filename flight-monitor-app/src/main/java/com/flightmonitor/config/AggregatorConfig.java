package com.flightmonitor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Configuration properties for the flight aggregator.
 */
@ConfigurationProperties(prefix = "flight.aggregator")
@Component
public class AggregatorConfig {

    private boolean enabled = false;
    private List<String> sources = List.of("amadeus");
    private int timeoutSeconds = 10;
    private boolean deduplicateByFlightNumber = true;
    private int maxResultsPerSource = 20;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getSources() {
        return sources;
    }

    public void setSources(List<String> sources) {
        this.sources = sources;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public boolean isDeduplicateByFlightNumber() {
        return deduplicateByFlightNumber;
    }

    public void setDeduplicateByFlightNumber(boolean deduplicateByFlightNumber) {
        this.deduplicateByFlightNumber = deduplicateByFlightNumber;
    }

    public int getMaxResultsPerSource() {
        return maxResultsPerSource;
    }

    public void setMaxResultsPerSource(int maxResultsPerSource) {
        this.maxResultsPerSource = maxResultsPerSource;
    }
}
