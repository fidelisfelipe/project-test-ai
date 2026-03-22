# API Reference

## Base URL

```
http://localhost:8080
```

## Flight Endpoints

### GET /api/v1/flights/search

Search for available flight offers.

**Parameters:**

| Name          | Type   | Required | Description                        |
|---------------|--------|----------|------------------------------------|
| origin        | String | Yes      | Origin IATA code (3 uppercase letters, e.g. BSB) |
| destination   | String | Yes      | Destination IATA code              |
| departureDate | Date   | Yes      | Departure date (YYYY-MM-DD), must be future |
| returnDate    | Date   | No       | Return date (YYYY-MM-DD)           |
| adults        | Int    | No       | Number of adults (default: 1)      |
| cabinClass    | Enum   | No       | ECONOMY, PREMIUM_ECONOMY, BUSINESS, FIRST |

**Response:** `200 OK` with array of `FlightOfferResponse`

### GET /api/v1/flights/history

Get price history for a route.

**Parameters:** `origin`, `destination`, `days` (default 30)

**Response:** `200 OK` with array of `PriceHistoryResponse`

### GET /api/v1/flights/report

Generate a price report.

**Parameters:** `origin`, `destination`, `startDate`, `endDate`

**Response:** `200 OK` with `FlightReportResponse`

## Alert Endpoints

### POST /api/v1/alerts

Create a price alert.

**Body:**
```json
{
  "userEmail": "user@example.com",
  "origin": "BSB",
  "destination": "LIS",
  "departureDate": "2025-06-01",
  "targetPrice": 299.99
}
```

**Response:** `201 Created` with `Location` header

### GET /api/v1/alerts?email={email}

Get all alerts for a user.

**Response:** `200 OK` with array of `PriceAlertResponse`

### DELETE /api/v1/alerts/{id}

Delete an alert.

**Response:** `204 No Content`

### PUT /api/v1/alerts/{id}/pause

Pause an active alert.

**Response:** `200 OK`

### PUT /api/v1/alerts/{id}/resume

Resume a paused alert.

**Response:** `200 OK`

## Error Responses

All errors return RFC 9457 `ProblemDetail` format:

```json
{
  "type": "https://flightmonitor.com/errors/flight-not-found",
  "title": "Flight Not Found",
  "status": 404,
  "detail": "No flight found with the given criteria",
  "timestamp": "2024-01-15T10:30:00Z"
}
```
