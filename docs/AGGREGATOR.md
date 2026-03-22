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
