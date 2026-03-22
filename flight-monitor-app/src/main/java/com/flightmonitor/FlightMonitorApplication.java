package com.flightmonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the Flight Price Monitor application.
 */
@SpringBootApplication
@EnableCaching
@EnableKafka
@EnableScheduling
public class FlightMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlightMonitorApplication.class, args);
    }
}
