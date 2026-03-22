# Layers and Communication

## Full Flow of a Flight Search

The following describes the complete lifecycle of a user-initiated flight search:

1. **User opens browser** → navigates to `http://localhost:8080/flights/search`
2. **FlightWebController** renders `flights/search.html` with the search form
3. **User submits form** → POST to `/flights/search`
4. **FlightWebController** creates a `FlightSearchRequest` record and calls `FlightSearchService.searchFlights()`
5. **FlightSearchServiceImpl** validates origin, destination, and departure date (IATA pattern, future date)
6. **FlightSearchServiceImpl** checks the Caffeine cache (`flightSearches`) — cache hit returns immediately
7. On cache miss: **FlightSearchProducer** publishes a `FlightSearchRequestMessage` to `flight.search.requests` Kafka topic with a `correlationId` in the header
8. **FlightSearchConsumer** (running on Virtual Thread) consumes the message
9. **FlightSearchConsumer** calls `AmadeusClientImpl.searchFlights()` which invokes the Amadeus REST API
10. **FlightSearchConsumer** publishes results to `flight.search.results` topic
11. **FlightSearchServiceImpl** also calls `AmadeusClient` directly for the synchronous response path
12. Results are sorted by price ascending
13. **FlightSearchServiceImpl** calls `FlightOfferRepository.save()` for each result
14. **PriceHistoryService.saveSnapshot()** is called to record min/max/avg prices
15. **AlertService.checkAlertsForRoute()** is called — if any active alert has `targetPrice >= currentMinPrice`, it is triggered
16. Triggered alerts publish a `PriceAlertMessage` to `price.alerts` via `PriceAlertProducer`
17. **SearchLog** is persisted with durationMs and result count
18. Results are returned to the web controller and rendered in `flights/results.html`

## Layer Separation

```
Controller Layer (REST + Thymeleaf)
        ↓
Service Layer (Business Logic)
        ↓
Repository Layer (Spring Data JPA)
        ↓
H2 Database

Service Layer ← → Kafka (async messaging)
Service Layer ← → AmadeusClient (external API)
```
