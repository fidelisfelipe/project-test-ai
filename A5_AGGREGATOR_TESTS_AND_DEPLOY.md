# Task A5 — Testes do Aggregator e Ativação no Render

> **Camada**: Qualidade + Deploy  
> **Pré-requisito**: Tasks `A1` a `A4` concluídas  
> **Esta é a última task do Aggregator**

---

## Parte A — Testes do AggregatorFlightClient

### Prompt para o Copilot

```
Crie `src/test/java/com/flightmonitor/client/AggregatorFlightClientTest.java`
com os seguintes testes usando JUnit 5 + Mockito:

### Setup:
```java
@ExtendWith(MockitoExtension.class)
class AggregatorFlightClientTest {

    @Mock SerpApiFlightClient serpApiClient;
    @Mock KiwiTequilaClient kiwiClient;
    @Mock AmadeusClientImpl amadeusClient;

    private AggregatorFlightClient aggregator;
    private AggregatorConfig config;

    @BeforeEach
    void setup() {
        config = new AggregatorConfig();
        config.setEnabled(true);
        config.setSources(List.of("serpapi", "kiwi"));
        config.setTimeoutSeconds(5);
        config.setDeduplicateByFlightNumber(true);

        // Usar ObjectProvider mocks ou passar clients diretamente
        // via construtor alternativo de teste
        aggregator = new AggregatorFlightClient(config, serpApiClient, kiwiClient, amadeusClient);
    }
}
```

### Testes:

1. `searchFlights() — agrega resultados de duas fontes`
   - serpApiClient retorna 2 ofertas (preços 3000, 3500)
   - kiwiClient retorna 2 ofertas (preços 2800, 4000)
   - Verifica que retorna 4 resultados ordenados: 2800, 3000, 3500, 4000

2. `searchFlights() — deduplica por número de voo mantendo menor preço`
   - serpApiClient retorna oferta: flightNumber="TP083", price=3200
   - kiwiClient retorna oferta: flightNumber="TP083", price=2900 (mesmo voo, mais barato)
   - Verifica que retorna 1 resultado com price=2900

3. `searchFlights() — falha de uma fonte não cancela a outra`
   - serpApiClient lança RuntimeException("timeout")
   - kiwiClient retorna 2 ofertas normalmente
   - Verifica que retorna 2 resultados (da Kiwi) sem lançar exception

4. `searchFlights() — retorna vazio quando nenhuma fonte está configurada`
   - config.setSources(List.of("amadeus")) mas amadeusClient não está disponível
   - Verifica retorno de lista vazia + log WARN

5. `searchFlights() — respeita timeout por fonte`
   - serpApiClient demora > timeout configurado
   - Verifica que a future é cancelada e lista vazia retornada para aquela fonte
   - Verifica que outras fontes ainda são consultadas

### Helper para criar FlightOffer de teste:
```java
private FlightOffer buildOffer(String flightNumber, double price, String source) {
    FlightOffer offer = new FlightOffer();
    offer.setId(source + "-" + flightNumber);
    offer.setFlightNumber(flightNumber);
    offer.setOrigin("GRU");
    offer.setDestination("LIS");
    offer.setPrice(BigDecimal.valueOf(price));
    offer.setCurrency("BRL");
    offer.setAirline("TP");
    offer.setDepartureDate(LocalDate.of(2026, 4, 15));
    offer.setSource(source);
    return offer;
}
```
```

---

## Parte B — Ativação no Render

### Prompt para o Copilot

```
Atualizar o Dockerfile e a documentação para suportar o profile aggregator.

### No Dockerfile, a variável de ambiente já definida:
```dockerfile
ENV SPRING_PROFILES_ACTIVE=sync
```
Não alterar — será sobrescrita por variável de ambiente no Render.

### Criar src/main/resources/application-aggregator.yml se não existir:
(conteúdo já definido na Task A1 — verificar se foi criado)

### Criar docs/AGGREGATOR.md:
```markdown
# Aggregator Flight Client

## Fontes disponíveis

| Source key | Implementação | Free tier |
|---|---|---|
| `serpapi` | SerpApiFlightClient | 250 queries/mês |
| `kiwi` | KiwiTequilaClient | Gratuito com registro |
| `amadeus` | AmadeusClientImpl | Sandbox gratuito |
| `mock` | MockAmadeusClient | Sempre disponível |

## Como ativar no Render

Variáveis de ambiente a adicionar no Web Service:

```
SPRING_PROFILES_ACTIVE=aggregator
SERPAPI_API_KEY=sua_chave
KIWI_API_KEY=sua_chave
KIWI_PARTNER=picky
```

## Como ativar localmente

```bash
export SPRING_PROFILES_ACTIVE=aggregator
export SERPAPI_API_KEY=sua_chave
export KIWI_API_KEY=sua_chave
mvn spring-boot:run -pl flight-monitor-app
```

## Como adicionar uma nova fonte futuramente

1. Criar `XyzFlightClient implements AmadeusClient`
2. Anotar com `@Component` e `@ConditionalOnProperty(prefix="xyz", name="api-key")`
3. Registrar no construtor do `AggregatorFlightClient` via `ObjectProvider<XyzFlightClient>`
4. Adicionar `"xyz"` como valor válido em `flight.aggregator.sources`
5. Criar `application-{profile}.yml` com as configs necessárias

Nenhum outro arquivo precisa ser alterado.
```
```

---

## Checklist Final do Aggregator

- [ ] `mvn test` passa todos os testes unitários dos 3 clients + aggregator
- [ ] Com `SPRING_PROFILES_ACTIVE=sync`, comportamento original inalterado
- [ ] Com `SPRING_PROFILES_ACTIVE=aggregator` + chaves configuradas, contexto sobe
- [ ] Se apenas `SERPAPI_API_KEY` estiver configurada, só SerpApi é consultada
- [ ] Se apenas `KIWI_API_KEY` estiver configurada, só Kiwi é consultada
- [ ] Sem nenhuma chave + profile aggregator: retorna lista vazia com WARN no log
- [ ] `docs/AGGREGATOR.md` criado e explica como adicionar nova fonte

## Variáveis de ambiente no Render para ativar

```
SPRING_PROFILES_ACTIVE=aggregator
SERPAPI_API_KEY=          ← obter em serpapi.com (250/mês grátis)
KIWI_API_KEY=             ← obter em tequila.kiwi.com (grátis)
KIWI_PARTNER=picky
```
