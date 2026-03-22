# Architecture

## Component Diagram

```
┌─────────────────────────────────────────────────────┐
│                  Maven Multi-Module                  │
│                                                     │
│  ┌──────────────────────┐  ┌─────────────────────┐  │
│  │  flight-monitor-app  │  │ flight-monitor-admin│  │
│  │                      │  │                     │  │
│  │  - Controllers       │  │  - Admin Server UI  │  │
│  │  - Services          │  │  - Health Dashboard │  │
│  │  - Kafka Producers   │  └─────────────────────┘  │
│  │  - Kafka Consumers   │                           │
│  │  - Repositories      │                           │
│  │  - Thymeleaf Views   │                           │
│  └──────────────────────┘                           │
└─────────────────────────────────────────────────────┘
```

## Maven Modules

### flight-monitor-app (Main Application)
The core Spring Boot application that handles:
- REST API endpoints (`/api/v1/`)
- Thymeleaf web views
- Kafka producer/consumer for async flight searches
- JPA repositories with H2 in dev, configurable in prod
- Spring Cache with Caffeine

### flight-monitor-admin (Admin Server)
A dedicated Spring Boot Admin Server that:
- Monitors the health and metrics of `flight-monitor-app`
- Provides a web dashboard at port 8081
- Secured with basic auth (admin/admin in dev)

## Architecture Decisions

| Decision | Choice | Reason |
|----------|--------|--------|
| Database (dev) | H2 file mode | Zero setup, persistent between restarts |
| Cache | Caffeine | In-memory, high performance, simple config |
| Messaging | Apache Kafka | Async, decoupled flight searches |
| Virtual Threads | Java 17 (cached thread pool) | High concurrency without thread pool tuning |
| API Docs | SpringDoc OpenAPI 3 | Auto-generated from annotations |
| UI | Thymeleaf + Bootstrap 5 | Server-side rendering, no SPA complexity |
