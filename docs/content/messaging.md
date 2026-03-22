# Messaging

## Kafka Topology

```
flight.search.requests (6 partitions)
    ├── Producer: FlightSearchProducer
    └── Consumer: FlightSearchConsumer (group: flight-monitor-group, concurrency=10)

flight.search.results (6 partitions)
    └── Producer: FlightSearchConsumer (after Amadeus call)

price.alerts (6 partitions)
    ├── Producer: PriceAlertProducer
    └── Consumer: PriceAlertConsumer (group: flight-monitor-alert-group)

Dead Letter Topics (automatic):
    flight.search.requests.DLT
    flight.search.results.DLT
    price.alerts.DLT
```

## Message Schemas

### FlightSearchRequestMessage
```json
{
  "correlationId": "uuid-string",
  "origin": "BSB",
  "destination": "LIS",
  "departureDate": "2025-06-01",
  "returnDate": null,
  "adults": 1,
  "cabinClass": "ECONOMY"
}
```

### PriceAlertMessage
```json
{
  "alertId": "uuid-string",
  "userEmail": "user@example.com",
  "origin": "BSB",
  "destination": "LIS",
  "departureDate": "2025-06-01",
  "targetPrice": 299.99,
  "currentPrice": 250.00,
  "triggeredAt": "2024-01-15T10:30:00"
}
```

## Error Handling

- **ErrorHandlingDeserializer** wraps the JSON deserializer to handle poison pills gracefully
- **DefaultErrorHandler** with `ExponentialBackOff`: 3 retries at 1s → 2s → 4s intervals
- After retries are exhausted, messages are routed to the `.DLT` topic
- Consumer uses `MANUAL_IMMEDIATE` ack mode — ack only happens after successful processing

## Scaling

- Concurrency of 10 on `flight.search.requests` consumer
- Virtual Threads via `Executors.newVirtualThreadPerTaskExecutor()` for non-blocking I/O
- 6 partitions per topic allows horizontal scaling with up to 6 consumer instances
