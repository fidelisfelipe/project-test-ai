# Task A1 — Aggregator: Visão Geral e Interfaces

> **Camada**: Domínio / Source  
> **Pré-requisito**: Projeto flight-monitor-app existente com `AmadeusClient` interface  
> **Próxima task**: `A2_SERPAPI_CLIENT.md`

---

## Contexto

Adicionar um `AggregatorFlightClient` que implementa a mesma interface `AmadeusClient`
já existente, agregando resultados de múltiplas fontes em paralelo e retornando
a lista unificada e deduplicada, ordenada por preço.

```
AmadeusClient (interface já existente)
       │
       ├── AmadeusClientImpl        (já existe)
       ├── MockAmadeusClient        (já existe)
       ├── SerpApiFlightClient      (novo — Task A2)
       ├── KiwiTequilaClient        (novo — Task A3)
       └── AggregatorFlightClient   (novo — Task A4, orquestra os anteriores)
```

A troca de provider continua sendo feita via `application-{profile}.yml` — nenhuma
alteração no `FlightSearchService` ou no pipeline existente.

---

## Prompt para o Copilot

```
No projeto flight-monitor-app, crie as seguintes adições de domínio
em `src/main/java/com/flightmonitor/`:

### 1. Enum: client/FlightSource.java

```java
public enum FlightSource {
    AMADEUS,
    SERPAPI_GOOGLE_FLIGHTS,
    KIWI_TEQUILA,
    MOCK,
    AGGREGATOR
}
```

### 2. Interface: client/FlightClient.java

Renomear ou criar um alias limpo para a interface atual `AmadeusClient`,
adicionando o campo `source` nos resultados. Se `AmadeusClient` já for
uma interface com métodos como `searchFlights(FlightSearchRequest)`,
mantenha-a intacta e apenas adicione um método default:

```java
default FlightSource getSource() {
    return FlightSource.AMADEUS;
}
```

### 3. Config: config/AggregatorConfig.java

```java
@ConfigurationProperties(prefix = "flight.aggregator")
@Component
public class AggregatorConfig {
    private boolean enabled = false;
    private List<String> sources = List.of("amadeus");
    private int timeoutSeconds = 10;
    private boolean deduplicateByFlightNumber = true;
    private int maxResultsPerSource = 20;
    // getters e setters
}
```

### 4. Atualizar application.yml — adicionar seção:

```yaml
flight:
  aggregator:
    enabled: false          # true ativa o AggregatorFlightClient
    sources:
      - amadeus             # fontes ativas (amadeus | serpapi | kiwi)
    timeout-seconds: 10
    deduplicate-by-flight-number: true
    max-results-per-source: 20
```

### 5. Atualizar application-sync.yml (ou profile equivalente):

```yaml
flight:
  aggregator:
    enabled: false
    sources:
      - mock
```

### 6. Criar application-aggregator.yml:

```yaml
spring:
  profiles:
    active: aggregator

flight:
  aggregator:
    enabled: true
    sources:
      - serpapi
      - kiwi
    timeout-seconds: 10
    deduplicate-by-flight-number: true
    max-results-per-source: 20

serpapi:
  api-key: ${SERPAPI_API_KEY:}
  currency: BRL
  language: pt-br

kiwi:
  api-key: ${KIWI_API_KEY:}
  partner: ${KIWI_PARTNER:picky}
  currency: BRL
  locale: pt-BR
  base-url: https://tequila-api.kiwi.com
```

Todos os arquivos devem compilar sem erros. Nenhuma lógica de negócio
alterada — apenas adições.
```

---

## Checklist de Validação

- [ ] `FlightSource` enum compilando
- [ ] `AggregatorConfig` com `@ConfigurationProperties` funcionando
- [ ] `application-aggregator.yml` presente na raiz de resources
- [ ] `mvn compile` sem erros
- [ ] Nenhum bean existente quebrado
