# Task A2 — SerpApi Google Flights Client

> **Camada**: Source Provider  
> **Pré-requisito**: `A1_AGGREGATOR_OVERVIEW.md` concluído  
> **Próxima task**: `A3_KIWI_TEQUILA_CLIENT.md`  
> **Registro gratuito**: https://serpapi.com (250 queries/mês no free tier)

---

## Contexto da API

Endpoint: `GET https://serpapi.com/search`  
Parâmetros principais:

| Parâmetro | Valor |
|---|---|
| `engine` | `google_flights` |
| `departure_id` | Código IATA (ex: `GRU`) |
| `arrival_id` | Código IATA (ex: `LIS`) |
| `outbound_date` | `YYYY-MM-DD` |
| `type` | `2` = one-way, `1` = round trip |
| `currency` | `BRL` |
| `hl` | `pt-br` |
| `api_key` | sua chave SerpApi |

Resposta: campos `best_flights` e `other_flights`, cada um com array de objetos contendo:
- `flights[].airline`, `flights[].flight_number`, `flights[].travel_class`
- `flights[].departure_airport.id`, `flights[].arrival_airport.id`
- `flights[].departure_airport.time` (formato `"YYYY-MM-DD HH:mm"`)
- `flights[].duration` (em minutos)
- `price` (inteiro, na moeda configurada)
- `total_duration` (minutos totais incluindo conexões)

---

## Prompt para o Copilot

```
Crie `src/main/java/com/flightmonitor/client/SerpApiFlightClient.java`
que implementa a interface `AmadeusClient` (ou equivalente) consumindo
a SerpApi Google Flights.

### Config class: client/config/SerpApiConfig.java

```java
@ConfigurationProperties(prefix = "serpapi")
@Component
public class SerpApiConfig {
    private String apiKey;
    private String currency = "BRL";
    private String language = "pt-br";
    private String baseUrl = "https://serpapi.com/search";
    // getters e setters
}
```

### Implementação: SerpApiFlightClient.java

**Anotações:**
- `@Component`
- `@ConditionalOnProperty(prefix = "serpapi", name = "api-key", matchIfMissing = false)`
- Implementa `AmadeusClient` (ou `FlightClient`)

**Método principal `searchFlights(FlightSearchRequest request)`:**

1. Monta os query params via `UriComponentsBuilder`:
   ```
   engine=google_flights
   departure_id={request.getOrigin()}
   arrival_id={request.getDestination()}
   outbound_date={request.getDepartureDate()}  // formato YYYY-MM-DD
   type=2  // one-way; se returnDate presente, usar type=1
   adults={request.getAdults()}
   travel_class={mapCabinClass(request.getCabinClass())}
   currency=BRL
   hl=pt-br
   no_cache=false
   api_key={config.getApiKey()}
   ```

2. Faz GET com `RestTemplate` ou `WebClient` (o que já existir no projeto)

3. Parseia resposta JSON — combinar `best_flights` e `other_flights`:

   ```java
   // Estrutura de resposta SerpApi (usar Jackson @JsonProperty):
   record SerpApiResponse(
       @JsonProperty("best_flights") List<SerpApiOffer> bestFlights,
       @JsonProperty("other_flights") List<SerpApiOffer> otherFlights,
       @JsonProperty("price_insights") SerpApiPriceInsights priceInsights
   ) {}

   record SerpApiOffer(
       List<SerpApiLeg> flights,
       Integer price,
       @JsonProperty("total_duration") Integer totalDuration,
       String type,
       @JsonProperty("airline_logo") String airlineLogo
   ) {}

   record SerpApiLeg(
       @JsonProperty("departure_airport") SerpApiAirport departureAirport,
       @JsonProperty("arrival_airport") SerpApiAirport arrivalAirport,
       Integer duration,
       String airline,
       @JsonProperty("flight_number") String flightNumber,
       @JsonProperty("travel_class") String travelClass,
       String airplane
   ) {}

   record SerpApiAirport(String name, String id, String time) {}
   ```

4. Mapeia cada `SerpApiOffer` para o tipo de retorno que `AmadeusClient` produz
   (provavelmente `FlightOffer` ou `FlightOfferResponse`):
   - `id` ← `"SERP-" + leg.flightNumber()`
   - `origin` ← primeiro leg `departureAirport.id()`
   - `destination` ← último leg `arrivalAirport.id()`
   - `airline` ← primeiro leg `airline()`
   - `price` ← `BigDecimal.valueOf(offer.price())`
   - `currency` ← `"BRL"`
   - `duration` ← formatar `totalDuration` em `"Xh Ym"`
   - `stops` ← `offer.flights().size() - 1`
   - `cabinClass` ← mapear string para `CabinClass` enum
   - `source` ← `"SERPAPI_GOOGLE_FLIGHTS"`
   - `departureDate` ← parsear `leg.departureAirport().time()` como `LocalDate`
   - `deepLink` ← `null` (SerpApi não retorna link direto de compra)
   - `metadata` ← `{ "airlineLogo": offer.airlineLogo(), "totalDurationMin": offer.totalDuration() }`

5. Retornar lista ordenada por preço asc, limitada a `config.getMaxResults()` (default 20)

**Método auxiliar `mapCabinClass`:**
```java
private int mapCabinClass(CabinClass cabin) {
    return switch (cabin) {
        case ECONOMY -> 1;
        case PREMIUM_ECONOMY -> 2;
        case BUSINESS -> 3;
        case FIRST -> 4;
        default -> 1;
    };
}
```

**Tratamento de erros:**
- HTTP 401: lançar `AmadeusApiException("SerpApi: API key inválida ou expirada")`
- HTTP 429: lançar `AmadeusApiException("SerpApi: rate limit atingido (250/mês no free tier)")`
- Timeout (> 15s): lançar `AmadeusApiException("SerpApi: timeout")`
- `best_flights` e `other_flights` ambos null/vazios: retornar lista vazia (sem exception)

**Logging:**
- INFO: `"[SerpApi] Searching {} -> {} on {}"` com origem, destino, data
- INFO: `"[SerpApi] Found {} results ({} best + {} other)"` com contagens
- WARN: quando price_insights sugere que o preço está acima da média típica

### Testes: SerpApiFlightClientTest.java

Criar em `src/test/java/com/flightmonitor/client/`:

1. `searchFlights() — mapeia best_flights corretamente`
   - Mock do RestTemplate retornando JSON fixture com 2 best_flights e 1 other_flight
   - Verifica que retorna 3 resultados ordenados por preço
   - Verifica mapeamento: stops = flights.size() - 1

2. `searchFlights() — retorna lista vazia quando API não encontra voos`
   - Mock retornando `{ "best_flights": null, "other_flights": null }`
   - Verifica que retorna lista vazia sem exception

3. `searchFlights() — lança exceção em 429`
   - Mock lançando HttpClientErrorException(429)
   - Verifica mensagem "rate limit"

**Fixture JSON** (criar em `src/test/resources/fixtures/serpapi_response.json`):
```json
{
  "best_flights": [
    {
      "flights": [
        {
          "departure_airport": { "name": "Guarulhos", "id": "GRU", "time": "2026-04-15 10:00" },
          "arrival_airport": { "name": "Lisboa", "id": "LIS", "time": "2026-04-15 22:30" },
          "duration": 750,
          "airline": "TAP Air Portugal",
          "flight_number": "TP 083",
          "travel_class": "Economy",
          "airplane": "Airbus A330"
        }
      ],
      "price": 2850,
      "total_duration": 750,
      "type": "One way",
      "airline_logo": "https://www.gstatic.com/flights/airline_logos/70px/TP.png"
    }
  ],
  "other_flights": [
    {
      "flights": [
        {
          "departure_airport": { "name": "Guarulhos", "id": "GRU", "time": "2026-04-15 14:00" },
          "arrival_airport": { "name": "Madrid", "id": "MAD", "time": "2026-04-16 04:00" },
          "duration": 660,
          "airline": "Iberia",
          "flight_number": "IB 6832",
          "travel_class": "Economy",
          "airplane": "Boeing 787"
        },
        {
          "departure_airport": { "name": "Madrid", "id": "MAD", "time": "2026-04-16 07:00" },
          "arrival_airport": { "name": "Lisboa", "id": "LIS", "time": "2026-04-16 08:05" },
          "duration": 65,
          "airline": "Iberia",
          "flight_number": "IB 3102",
          "travel_class": "Economy",
          "airplane": "Airbus A320"
        }
      ],
      "price": 3200,
      "total_duration": 785,
      "type": "One way"
    }
  ],
  "price_insights": {
    "lowest_price": 2850,
    "price_level": "typical",
    "typical_price_range": [2500, 4500]
  }
}
```
```

---

## Variáveis de ambiente necessárias no Render

```
SERPAPI_API_KEY=sua_chave_serpapi
```

## Checklist de Validação

- [ ] `SerpApiFlightClient` compila sem erros
- [ ] `@ConditionalOnProperty` não quebra o contexto quando `SERPAPI_API_KEY` está ausente
- [ ] Testes unitários passam com fixture JSON
- [ ] Mapeamento de `stops` correto (1 leg = 0 stops, 2 legs = 1 stop)
- [ ] Ordenação por preço funcionando
