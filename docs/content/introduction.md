# Introduction

Flight Price Monitor is a real-time flight price tracking platform built with Java 21, Spring Boot 3.3, Apache Kafka, and Thymeleaf.

## What It Does

- Searches for live flight offers via the Amadeus API
- Tracks historical price trends for routes
- Triggers price alerts when your target price is reached
- Provides a REST API and a web dashboard

## Architecture Overview

```
┌─────────────┐     REST/Thymeleaf     ┌────────────────────┐
│   Browser   │ ──────────────────────▶│ flight-monitor-app │
└─────────────┘                        │  (port 8080)       │
                                       └────────┬───────────┘
                                                │  Kafka
                                       ┌────────▼───────────┐
                                       │    Apache Kafka     │
                                       │  (port 9092)       │
                                       └────────┬───────────┘
                                                │
                                       ┌────────▼───────────┐
                                       │   Amadeus API      │
                                       └────────────────────┘

┌─────────────────────┐
│ flight-monitor-admin│ ◀── Spring Boot Admin Client
│   (port 8081)       │
└─────────────────────┘
```

## Quick Start

```bash
# 1. Start Kafka with Docker Compose
docker-compose -f docker/docker-compose.yml up -d zookeeper kafka

# 2. Run the application
mvn spring-boot:run -pl flight-monitor-app

# 3. Open the web UI
open http://localhost:8080
```

## Service Ports

| Service              | Port | URL                             |
|----------------------|------|---------------------------------|
| flight-monitor-app   | 8080 | http://localhost:8080           |
| flight-monitor-admin | 8081 | http://localhost:8081           |
| Kafka                | 9092 | localhost:9092                  |
| Kafka UI             | 8090 | http://localhost:8090           |
| H2 Console           | 8080 | http://localhost:8080/h2-console|
| Swagger UI           | 8080 | http://localhost:8080/swagger-ui.html |
