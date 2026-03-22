# Task A3 — Kiwi Tequila Client

> **Camada**: Source Provider  
> **Pré-requisito**: `A1_AGGREGATOR_OVERVIEW.md` concluído  
> **Próxima task**: `A4_AGGREGATOR_CLIENT.md`  
> **Registro gratuito**: https://tequila.kiwi.com (free tier disponível)

---

## Contexto da API

Base URL: `https://tequila-api.kiwi.com`  
Header obrigatório: `apikey: {sua_chave}`

Endpoint de busca: `GET /v2/search`

| Parâmetro | Valor |
|---|---|
| `fly_from` | Código IATA (ex: `GRU`) |
| `fly_to` | Código IATA (ex: `LIS`) |
| `dateFrom` | `DD/MM/YYYY` |
| `dateTo` | `DD/MM/YYYY` (mesma data = exato) |
| `adults` | número de adultos |
| `selected_cabins` | `M`=economy, `W`=premium, `C`=business, `F`=first |
| `curr` | `BRL` |
| `locale` | `pt-BR` |
| `partner` | código do parceiro (use `picky` para dev) |
| `limit` | máx resultados (default 20) |
| `sort` | `price` para ordenar por preço |

Resposta JSON — campo principal `data` com array de itinerários:
```json
{
  "data": [
    {
      "id": "string",
      "flyFrom": "GRU",
      "flyTo": "LIS",
      "cityFrom": "São Paulo",
      "cityTo": "Lisboa",
      "airlines": ["TP"],
      "price": 2750,
      "currency": "BRL",
      "duration": { "departure": 43200, "return": 0, "total": 43200 },
      "route": [
        {
          "id": "string",
          "flyFrom": "GRU",
          "flyTo": "LIS",
          "airline": "TP",
          "flight_no": 83,
          "operating_carrier": "TP",
          "equipment": "333",
          "local_departure": "2026-04-15T10:00:00.000Z",
          "local_arrival": "2026-04-15T22:30:00.000Z",
          "fare_classes": "Y",
          "fare_category": "M"
        }
      ],
      "deep_link": "https://www.kiwi.com/deep?..."
    }
  ],
  "currency": "BRL",
  "_results": 15
}
```

---

## Prompt para o Copilot

```
Crie `src/main/java/com/flightmonitor/client/KiwiTequilaClient.java`
que implementa a interface `AmadeusClient` (ou equivalente) consumindo
a Kiwi Tequila API.

### Config class: client/config/KiwiConfig.java

```java
@ConfigurationProperties(prefix = "kiwi")
@Component
public class KiwiConfig {
    private String apiKey;
    private String partner = "picky";
    private String currency = "BRL";
    private String locale = "pt-BR";
    private String baseUrl = "https://tequila-api.kiwi.com";
    private int limit = 20;
    // getters e setters
}
```

### Implementação: KiwiTequilaClient.java

**Anotações:**
- `@Component`
- `@ConditionalOnProperty(prefix = "kiwi", name = "api-key", matchIfMissing = false)`
- Implementa `AmadeusClient` (ou `FlightClient`)

**Método principal `searchFlights(FlightSearchRequest request)`:**

1. Monta os parâmetros:
   ```java
   String dateFormatted = request.getDepartureDate()
       .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

   UriComponentsBuilder.fromHttpUrl(config.getBaseUrl() + "/v2/search")
       .queryParam("fly_from", request.getOrigin())
       .queryParam("fly_to", request.getDestination())
       .queryParam("dateFrom", dateFormatted)
       .queryParam("dateTo", dateFormatted)
       .queryParam("adults", request.getAdults())
       .queryParam("selected_cabins", mapCabin(request.getCabinClass()))
       .queryParam("curr", config.getCurrency())
       .queryParam("locale", config.getLocale())
       .queryParam("partner", config.getPartner())
       .queryParam("limit", config.getLimit())
       .queryParam("sort", "price")
   ```

2. Header obrigatório: `apikey: {config.getApiKey()}`

3. Parsear resposta — DTOs internos:

   ```java
   record KiwiResponse(
       List<KiwiItinerary> data,
       String currency,
       @JsonProperty("_results") Integer results
   ) {}

   record KiwiItinerary(
       String id,
       String flyFrom,
       String flyTo,
       String cityFrom,
       String cityTo,
       List<String> airlines,
       Double price,
       KiwiDuration duration,
       List<KiwiRoute> route,
       @JsonProperty("deep_link") String deepLink
   ) {}

   record KiwiDuration(
       Long departure,
       @JsonProperty("return") Long returnDuration,
       Long total
   ) {}

   record KiwiRoute(
       String flyFrom,
       String flyTo,
       String airline,
       @JsonProperty("flight_no") Integer flightNo,
       @JsonProperty("local_departure") String localDeparture,
       @JsonProperty("local_arrival") String localArrival,
       String equipment
   ) {}
   ```

4. Mapeia cada `KiwiItinerary` para o tipo de retorno de `AmadeusClient`:
   - `id` ← `"KIWI-" + itinerary.id()`
   - `origin` ← `itinerary.flyFrom()`
   - `destination` ← `itinerary.flyTo()`
   - `airline` ← `String.join("/", itinerary.airlines())`
   - `price` ← `BigDecimal.valueOf(itinerary.price())`
   - `currency` ← `itinerary.currency()` ou `config.getCurrency()`
   - `duration` ← formatar `itinerary.duration().departure() / 60` como `"Xh Ym"`
   - `stops` ← `itinerary.route().size() - 1`
   - `cabinClass` ← reverter `mapCabin()` para `CabinClass` enum
   - `source` ← `"KIWI_TEQUILA"`
   - `departureDate` ← `request.getDepartureDate()`
   - `deepLink` ← `itinerary.deepLink()` — Kiwi fornece link direto de compra!
   - `metadata` ← `{ "cityFrom": cityFrom, "cityTo": cityTo, "kiwiId": id }`

5. Retornar lista ordenada por preço asc (já vem ordenada da API com `sort=price`,
   mas reordenar localmente para garantia)

**Método auxiliar `mapCabin`:**
```java
private String mapCabin(CabinClass cabin) {
    return switch (cabin) {
        case ECONOMY -> "M";
        case PREMIUM_ECONOMY -> "W";
        case BUSINESS -> "C";
        case FIRST -> "F";
        default -> "M";
    };
}
```

**Tratamento de erros:**
- HTTP 403 com body `"unknown partner"`: lançar `AmadeusApiException("Kiwi: parceiro inválido — registre-se em tequila.kiwi.com")`
- HTTP 403 com body `"'apikey' header is required"`: lançar `AmadeusApiException("Kiwi: API key ausente ou inválida")`
- `data` null ou vazio: retornar lista vazia (sem exception — rota sem resultados é válida)
- Timeout (> 15s): lançar `AmadeusApiException("Kiwi: timeout")`

**Logging:**
- INFO: `"[Kiwi] Searching {} -> {} on {} ({} adults)"` 
- INFO: `"[Kiwi] Found {} results, cheapest: {} {}"` com preço e moeda
- DEBUG: deep_link do resultado mais barato (útil para debug manual)

### Testes: KiwiTequilaClientTest.java

Criar em `src/test/java/com/flightmonitor/client/`:

1. `searchFlights() — mapeia itinerário direto (1 route = 0 stops)`
   - Mock retornando fixture com 1 itinerário, 1 rota
   - Verifica stops = 0, deepLink não nulo, airline = "TP"

2. `searchFlights() — mapeia itinerário com escala (2 routes = 1 stop)`
   - Fixture com 2 rotas no itinerário
   - Verifica stops = 1, airline = "TP/IB"

3. `searchFlights() — retorna vazio para rota sem resultado`
   - Mock retornando `{ "data": [], "_results": 0 }`
   - Verifica retorno de lista vazia sem exception

4. `searchFlights() — lança exceção descritiva em 403`
   - Mock retornando 403 com body `"unknown partner provided"`
   - Verifica mensagem de erro contém "parceiro inválido"

**Fixture JSON** (criar em `src/test/resources/fixtures/kiwi_response.json`):
```json
{
  "data": [
    {
      "id": "abc123",
      "flyFrom": "GRU",
      "flyTo": "LIS",
      "cityFrom": "São Paulo",
      "cityTo": "Lisboa",
      "airlines": ["TP"],
      "price": 2750.0,
      "currency": "BRL",
      "duration": { "departure": 45000, "return": 0, "total": 45000 },
      "route": [
        {
          "flyFrom": "GRU",
          "flyTo": "LIS",
          "airline": "TP",
          "flight_no": 83,
          "local_departure": "2026-04-15T10:00:00.000Z",
          "local_arrival": "2026-04-15T22:30:00.000Z",
          "equipment": "333"
        }
      ],
      "deep_link": "https://www.kiwi.com/deep?flightsId=abc123&lang=pt&currency=BRL"
    }
  ],
  "currency": "BRL",
  "_results": 1
}
```
```

---

## Variáveis de ambiente necessárias no Render

```
KIWI_API_KEY=sua_chave_kiwi
KIWI_PARTNER=picky
```

## Checklist de Validação

- [ ] `KiwiTequilaClient` compila sem erros
- [ ] Header `apikey` sendo enviado (não como query param)
- [ ] `dateFrom` e `dateTo` no formato `DD/MM/YYYY` (não ISO)
- [ ] `deepLink` populado no resultado — diferencial do Kiwi
- [ ] Testes passam com fixture
- [ ] `@ConditionalOnProperty` não quebra contexto sem `KIWI_API_KEY`
