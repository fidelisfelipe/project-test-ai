# Task A4 — AggregatorFlightClient

> **Camada**: Source Provider / Orquestração  
> **Pré-requisito**: Tasks `A1`, `A2` e `A3` concluídas  
> **Próxima task**: `A5_AGGREGATOR_CONFIG_AND_TESTS.md`

---

## Contexto

O `AggregatorFlightClient` implementa a mesma interface `AmadeusClient` e é ativado
quando `flight.aggregator.enabled=true`. Ele orquestra chamadas paralelas aos clients
ativos, combina os resultados, deduplica por número de voo e retorna ordenado por preço.

O `FlightSearchService` existente não precisa de nenhuma alteração — ele continua
chamando `AmadeusClient.searchFlights()` normalmente.

---

## Prompt para o Copilot

```
Crie `src/main/java/com/flightmonitor/client/AggregatorFlightClient.java`.

### Anotações:
- `@Component`
- `@Primary` — tem precedência sobre os outros clients quando presente
- `@ConditionalOnProperty(prefix = "flight.aggregator", name = "enabled", havingValue = "true")`
- Implementa `AmadeusClient` (ou `FlightClient`)

### Construtor — injeção dos clients disponíveis:

```java
public AggregatorFlightClient(
    AggregatorConfig config,
    ObjectProvider<AmadeusClientImpl> amadeusClient,
    ObjectProvider<SerpApiFlightClient> serpApiClient,
    ObjectProvider<KiwiTequilaClient> kiwiClient,
    ObjectProvider<MockAmadeusClient> mockClient
) {
    // Montar map de clients disponíveis
    // Usar ObjectProvider para evitar falha quando bean não está presente
}
```

Montar internamente um `Map<String, AmadeusClient>` com os clients disponíveis:
```java
Map<String, AmadeusClient> availableClients = new LinkedHashMap<>();
amadeusClient.ifAvailable(c -> availableClients.put("amadeus", c));
serpApiClient.ifAvailable(c -> availableClients.put("serpapi", c));
kiwiClient.ifAvailable(c -> availableClients.put("kiwi", c));
mockClient.ifAvailable(c -> availableClients.put("mock", c));
```

### Método principal `searchFlights(FlightSearchRequest request)`:

1. Filtrar `availableClients` pelas fontes configuradas em `config.getSources()`:
   ```java
   List<AmadeusClient> activeClients = config.getSources().stream()
       .filter(availableClients::containsKey)
       .map(availableClients::get)
       .toList();
   ```
   Se `activeClients` vazio: logar WARN e retornar lista vazia.

2. Executar chamadas em paralelo com `CompletableFuture` e timeout:
   ```java
   List<CompletableFuture<List<FlightOffer>>> futures = activeClients.stream()
       .map(client -> CompletableFuture
           .supplyAsync(() -> safeSearch(client, request))
           .orTimeout(config.getTimeoutSeconds(), TimeUnit.SECONDS)
           .exceptionally(ex -> {
               log.warn("[Aggregator] Source {} failed: {}", 
                   client.getClass().getSimpleName(), ex.getMessage());
               return List.of();
           }))
       .toList();
   ```

3. Aguardar todas as futures:
   ```java
   CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
   List<FlightOffer> allResults = futures.stream()
       .flatMap(f -> f.join().stream())
       .collect(Collectors.toList());
   ```

4. Deduplicar se `config.isDeduplicateByFlightNumber()`:
   - Chave de deduplicação: `flightNumber + departureDate + origin + destination`
   - Em caso de duplicata, manter o de menor preço
   ```java
   Map<String, FlightOffer> deduplicated = new LinkedHashMap<>();
   for (FlightOffer offer : allResults) {
       String key = buildDeduplicationKey(offer);
       deduplicated.merge(key, offer, 
           (existing, incoming) -> existing.getPrice()
               .compareTo(incoming.getPrice()) <= 0 ? existing : incoming);
   }
   ```

5. Ordenar por preço asc e retornar:
   ```java
   return deduplicated.values().stream()
       .sorted(Comparator.comparing(FlightOffer::getPrice))
       .collect(Collectors.toList());
   ```

### Método auxiliar `safeSearch`:
```java
private List<FlightOffer> safeSearch(AmadeusClient client, FlightSearchRequest req) {
    try {
        log.info("[Aggregator] Querying source: {}", client.getClass().getSimpleName());
        List<FlightOffer> results = client.searchFlights(req);
        log.info("[Aggregator] Source {} returned {} results",
            client.getClass().getSimpleName(), results.size());
        return results;
    } catch (Exception e) {
        log.warn("[Aggregator] Source {} threw: {}",
            client.getClass().getSimpleName(), e.getMessage());
        return List.of();
    }
}
```

### Método auxiliar `buildDeduplicationKey`:
```java
private String buildDeduplicationKey(FlightOffer offer) {
    // Para voos diretos: flightNumber é suficiente + data
    // Para conexões: usar origin + destination + departureDate
    String flightRef = offer.getFlightNumber() != null && !offer.getFlightNumber().isBlank()
        ? offer.getFlightNumber()
        : offer.getOrigin() + offer.getDestination();
    return flightRef + "|" + offer.getDepartureDate() + "|" + offer.getAirline();
}
```

### Logging do relatório final:
```java
log.info("[Aggregator] Completed: {} total → {} after dedup, cheapest: {} {}",
    allResults.size(),
    deduplicated.size(),
    deduplicated.values().stream().mapToDouble(o -> o.getPrice().doubleValue()).min().orElse(0),
    "BRL");
```
```

---

## Checklist de Validação

- [ ] `@Primary` + `@ConditionalOnProperty` coexistindo sem conflito de beans
- [ ] `ObjectProvider` não falha quando algum client está ausente (sem API key)
- [ ] Chamadas paralelas — dois clients não bloqueiam um ao outro
- [ ] Falha de um client não cancela os demais
- [ ] Deduplicação mantém o menor preço entre duplicatas
- [ ] Log do relatório final aparece no console com contagens corretas
