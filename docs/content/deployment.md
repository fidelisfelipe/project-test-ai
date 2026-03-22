# Deployment

## Docker Image Generation

Images are built using Spring Boot Buildpacks (no Dockerfile required):

```bash
# Build the app image
mvn spring-boot:build-image -pl flight-monitor-app \
  -Dspring-boot.build-image.imageName=flight-monitor-app:latest

# Build the admin image
mvn spring-boot:build-image -pl flight-monitor-admin \
  -Dspring-boot.build-image.imageName=flight-monitor-admin:latest
```

## GitHub Container Registry (GHCR)

CI/CD automatically pushes images to GHCR on commits to `main` or `develop`:

```
ghcr.io/<owner>/flight-monitor-app:<sha>
ghcr.io/<owner>/flight-monitor-admin:<sha>
```

## Docker Compose (Local)

```bash
# Start all services
docker-compose -f docker/docker-compose.yml up -d

# Check logs
docker-compose -f docker/docker-compose.yml logs -f flight-monitor-app

# Stop all services
docker-compose -f docker/docker-compose.yml down
```

## Environment Variables

| Variable            | Default                   | Description                  |
|---------------------|---------------------------|------------------------------|
| KAFKA_BOOTSTRAP_SERVERS | localhost:9092        | Kafka broker address         |
| AMADEUS_API_KEY     | test-key                  | Amadeus API key              |
| AMADEUS_API_SECRET  | test-secret               | Amadeus API secret           |
| AMADEUS_HOST        | test.api.amadeus.com      | Amadeus API hostname         |
| ADMIN_SERVER_URL    | http://localhost:8081     | Spring Boot Admin Server URL |
| ALERT_EMAIL_ENABLED | false                     | Enable email alert delivery  |

## Spring Boot Admin Operations

Access the admin dashboard at `http://localhost:8081` with credentials `admin/admin`.

From the dashboard you can:
- View health status of all registered instances
- Inspect environment properties
- View log levels and change them dynamically
- Monitor JVM metrics (heap, threads, GC)
- View Kafka consumer lag metrics via actuator endpoints
