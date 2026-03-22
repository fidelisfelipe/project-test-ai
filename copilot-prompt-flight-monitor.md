# GitHub Copilot — Prompt Completo: Flight Price Monitor

> Cole este prompt no GitHub Copilot Chat (modo Agent) ou no Copilot Workspace.
> Linguagem de instrução: Português. Linguagem do código gerado: Inglês.

---

## CONTEXTO GERAL

Cria um projecto Java completo chamado **`flight-price-monitor`** que monitoriza preços de passagens aéreas em tempo real entre aeroportos usando a API Amadeus. O projecto deve ser production-ready, bem documentado, testado, containerizado e com pipeline CI/CD completa.

---

## STACK TECNOLÓGICA OBRIGATÓRIA

- **Java 21** (Virtual Threads / Project Loom onde aplicável)
- **Spring Boot 3.3.x** com todos os starters necessários
- **Thymeleaf** para server-side rendering do frontend
- **H2** como banco de dados em memória (modo file para persistência entre restarts em dev)
- **Spring Data JPA** com Hibernate
- **Maven** como gestor de dependências (sem Gradle)
- **SpringDoc OpenAPI 3 / Swagger UI** para documentação da API REST
- **Spring Boot Admin** (servidor dedicado + cliente) para dashboard administrativa
- **Apache Kafka** como camada de mensageria (Docker Compose para local)
- **Znai** para documentação técnica do projecto
- **Spring Boot Maven Plugin** com geração de imagem Docker via Buildpacks (`spring-boot:build-image`)
- **JUnit 5 + Mockito + AssertJ** para testes unitários (cobertura mínima 50% via JaCoCo)
- **REST Assured + Testcontainers** para testes de smoke/integração
- **GitHub Actions** para pipeline CI/CD completa

---

## ESTRUTURA DO PROJECTO

Gera a seguinte estrutura de módulos Maven multi-módulo:

```
flight-price-monitor/                    ← parent POM
├── flight-monitor-app/                  ← aplicação principal (Spring Boot)
│   ├── src/main/java/com/flightmonitor/
│   │   ├── FlightMonitorApplication.java
│   │   ├── config/
│   │   │   ├── AmadeusConfig.java
│   │   │   ├── KafkaConfig.java
│   │   │   ├── SwaggerConfig.java
│   │   │   ├── CacheConfig.java
│   │   │   └── SecurityConfig.java
│   │   ├── controller/
│   │   │   ├── FlightSearchController.java      ← REST API
│   │   │   ├── FlightWebController.java         ← Thymeleaf views
│   │   │   ├── AlertController.java
│   │   │   └── ReportController.java
│   │   ├── service/
│   │   │   ├── FlightSearchService.java
│   │   │   ├── FlightSearchServiceImpl.java
│   │   │   ├── AlertService.java
│   │   │   ├── AlertServiceImpl.java
│   │   │   ├── PriceHistoryService.java
│   │   │   └── ReportService.java
│   │   ├── messaging/
│   │   │   ├── FlightSearchProducer.java
│   │   │   ├── FlightSearchConsumer.java
│   │   │   ├── PriceAlertProducer.java
│   │   │   ├── PriceAlertConsumer.java
│   │   │   └── dto/
│   │   │       ├── FlightSearchRequestMessage.java
│   │   │       └── PriceAlertMessage.java
│   │   ├── domain/
│   │   │   ├── entity/
│   │   │   │   ├── FlightOffer.java
│   │   │   │   ├── PriceHistory.java
│   │   │   │   ├── PriceAlert.java
│   │   │   │   └── SearchLog.java
│   │   │   └── enums/
│   │   │       ├── AlertStatus.java
│   │   │       └── CabinClass.java
│   │   ├── repository/
│   │   │   ├── FlightOfferRepository.java
│   │   │   ├── PriceHistoryRepository.java
│   │   │   ├── PriceAlertRepository.java
│   │   │   └── SearchLogRepository.java
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   │   ├── FlightSearchRequest.java
│   │   │   │   └── CreateAlertRequest.java
│   │   │   └── response/
│   │   │       ├── FlightOfferResponse.java
│   │   │       ├── PriceHistoryResponse.java
│   │   │       └── FlightReportResponse.java
│   │   ├── mapper/
│   │   │   ├── FlightOfferMapper.java
│   │   │   └── PriceAlertMapper.java
│   │   ├── client/
│   │   │   ├── AmadeusClient.java
│   │   │   └── AmadeusClientImpl.java
│   │   └── exception/
│   │       ├── GlobalExceptionHandler.java
│   │       ├── FlightNotFoundException.java
│   │       └── AmadeusApiException.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── application-dev.yml
│   │   ├── application-prod.yml
│   │   ├── templates/
│   │   │   ├── layout/
│   │   │   │   └── base.html
│   │   │   ├── flights/
│   │   │   │   ├── search.html
│   │   │   │   ├── results.html
│   │   │   │   └── history.html
│   │   │   ├── alerts/
│   │   │   │   ├── list.html
│   │   │   │   └── create.html
│   │   │   ├── reports/
│   │   │   │   └── dashboard.html
│   │   │   └── error/
│   │   │       ├── 404.html
│   │   │       └── 500.html
│   │   └── static/
│   │       ├── css/
│   │       │   └── app.css
│   │       └── js/
│   │           └── app.js
│   └── src/test/java/com/flightmonitor/
│       ├── unit/
│       │   ├── service/
│       │   │   ├── FlightSearchServiceTest.java
│       │   │   ├── AlertServiceTest.java
│       │   │   └── PriceHistoryServiceTest.java
│       │   ├── controller/
│       │   │   ├── FlightSearchControllerTest.java
│       │   │   └── AlertControllerTest.java
│       │   └── messaging/
│       │       ├── FlightSearchProducerTest.java
│       │       └── FlightSearchConsumerTest.java
│       └── smoke/
│           ├── FlightSearchSmokeTest.java
│           ├── AlertSmokeTest.java
│           └── KafkaSmokeTest.java
├── flight-monitor-admin/                ← Spring Boot Admin Server
│   └── src/main/java/com/flightmonitor/admin/
│       └── AdminServerApplication.java
├── docs/                                ← Znai documentation
│   ├── znai.config
│   └── content/
│       ├── introduction.md
│       ├── architecture.md
│       ├── layers-communication.md
│       ├── messaging.md
│       ├── api-reference.md
│       ├── adding-new-feature.md
│       └── deployment.md
├── docker/
│   └── docker-compose.yml              ← Kafka + Zookeeper + H2 Console + Admin
└── .github/
    └── workflows/
        ├── ci.yml
        ├── cd.yml
        └── smoke-tests.yml
```

---

## ESPECIFICAÇÕES DETALHADAS POR CAMADA

### 1. PARENT POM (`pom.xml`)

```xml
<!-- Gera o parent POM com: -->
<!-- - groupId: com.flightmonitor -->
<!-- - artifactId: flight-price-monitor -->
<!-- - version: 1.0.0-SNAPSHOT -->
<!-- - packaging: pom -->
<!-- - modules: flight-monitor-app, flight-monitor-admin -->
<!-- - Spring Boot BOM import: 3.3.x -->
<!-- - Java 21 compiler configuration com --enable-preview -->
<!-- - Plugin management: -->
<!--   - spring-boot-maven-plugin com layers e buildpacks -->
<!--   - maven-surefire-plugin 3.x com argLine do JaCoCo -->
<!--   - maven-failsafe-plugin para testes IT -->
<!--   - jacoco-maven-plugin: prepare-agent, report, check (50% minimum) -->
<!--   - maven-compiler-plugin: Java 21, annotation processors -->
<!-- - Dependências comuns no dependencyManagement: -->
<!--   - spring-kafka -->
<!--   - springdoc-openapi-starter-webmvc-ui:2.5.x -->
<!--   - de.codecentric:spring-boot-admin-starter-server:3.3.x -->
<!--   - de.codecentric:spring-boot-admin-starter-client:3.3.x -->
<!--   - org.testcontainers:kafka -->
<!--   - io.rest-assured:rest-assured -->
<!--   - com.amadeus:amadeus-java:8.0.0 -->
```

### 2. APPLICATION MODULE — `application.yml`

```yaml
# Gera application.yml com:

spring:
  application:
    name: flight-monitor-app
  
  datasource:
    url: jdbc:h2:file:./data/flightdb;AUTO_SERVER=TRUE
    driver-class-name: org.h2.Driver
    username: sa
    password: ""
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    open-in-view: false
  
  h2:
    console:
      enabled: true
      path: /h2-console
  
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
      properties:
        enable.idempotence: true
        max.in.flight.requests.per.connection: 5
    consumer:
      group-id: flight-monitor-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.flightmonitor.*"
    listener:
      concurrency: 10    # suporta muitas requisições simultâneas
      ack-mode: MANUAL_IMMEDIATE
  
  boot:
    admin:
      client:
        url: ${ADMIN_SERVER_URL:http://localhost:8081}
        instance:
          metadata:
            tags:
              environment: dev

  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=500,expireAfterWrite=300s

management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: always
    env:
      post:
        enabled: true    # permite alterar variáveis via Admin
  info:
    env:
      enabled: true

springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    operationsSorter: method
    tagsSorter: alpha

amadeus:
  api:
    key: ${AMADEUS_API_KEY:test-key}
    secret: ${AMADEUS_API_SECRET:test-secret}
    host: ${AMADEUS_HOST:test.api.amadeus.com}

flight:
  monitor:
    default-currency: EUR
    max-results: 20
    cache-ttl-minutes: 5
    alert:
      check-interval-minutes: 30
      email-enabled: ${ALERT_EMAIL_ENABLED:false}

server:
  port: 8080
  compression:
    enabled: true
  http2:
    enabled: true
  tomcat:
    threads:
      max: 200
      min-spare: 20
```

### 3. ENTIDADES JPA

```java
// FlightOffer.java
// Campos: id (UUID), origin (String), destination (String),
//         departureDate (LocalDate), returnDate (LocalDate nullable),
//         airline (String), price (BigDecimal), currency (String),
//         stops (int), duration (String), cabinClass (CabinClass enum),
//         deepLink (String), capturedAt (LocalDateTime auto),
//         source (String - "AMADEUS")
// Anotações: @Entity, @Table(indexes em origin+destination+departureDate),
//            @CreationTimestamp, @Column(precision=10,scale=2)

// PriceHistory.java  
// Campos: id (UUID), origin, destination, departureDate,
//         minPrice (BigDecimal), maxPrice, avgPrice,
//         offersCount (int), recordedAt (LocalDateTime),
//         trend (String - "UP"/"DOWN"/"STABLE")
// Índice composto: origin + destination + departureDate + recordedAt

// PriceAlert.java
// Campos: id (UUID), userEmail, origin, destination,
//         departureDate, targetPrice (BigDecimal),
//         status (AlertStatus enum), triggeredAt (nullable),
//         createdAt, lastCheckedAt
// AlertStatus enum: ACTIVE, TRIGGERED, EXPIRED, PAUSED

// SearchLog.java
// Campos: id (UUID), origin, destination, departureDate,
//         executedAt, resultsCount, durationMs (long),
//         source (CACHE/API/KAFKA), errorMessage (nullable)
```

### 4. CAMADA DE SERVIÇO

```java
// FlightSearchServiceImpl.java — implementa:
//
// searchFlights(FlightSearchRequest req):
//   1. Valida request (origem/destino IATA 3 letras, data futura)
//   2. Verifica cache (Caffeine) pela chave "origin-dest-date-adults"
//   3. Se cache miss: publica FlightSearchRequestMessage no Kafka
//      tópico "flight.search.requests"
//   4. Aguarda resposta via CompletableFuture com timeout 30s
//   5. Guarda resultado no H2 (FlightOffer entities)
//   6. Guarda PriceHistory snapshot
//   7. Verifica PriceAlerts activos para este par origem/destino
//   8. Registra SearchLog com métricas de performance
//   9. Retorna lista de FlightOfferResponse ordenada por preço
//
// Usar Virtual Threads (Java 21): @Bean TaskExecutor com
// Executors.newVirtualThreadPerTaskExecutor() para chamadas à API Amadeus
//
// AlertServiceImpl.java — implementa:
//   checkAlertsForRoute(String origin, String destination, BigDecimal currentMinPrice):
//     - Busca alertas ACTIVE para este par
//     - Se currentMinPrice <= targetPrice: actualiza status para TRIGGERED
//     - Publica PriceAlertMessage no tópico "price.alerts"
//     - Consumer do alerta envia email (mockado em dev, real em prod)
```

### 5. CAMADA DE MENSAGERIA (KAFKA)

```java
// KafkaConfig.java — configura:
//   - 3 tópicos: "flight.search.requests", "flight.search.results", "price.alerts"
//   - Cada tópico: 6 partições, replication factor 1 (dev) / 3 (prod)
//   - Dead Letter Topic automático para erros: sufixo ".DLT"
//   - ErrorHandlingDeserializer para evitar poison pills
//   - DefaultErrorHandler com BackOff exponencial (3 tentativas, 1s/2s/4s)
//
// FlightSearchProducer.java:
//   @Component com KafkaTemplate<String, FlightSearchRequestMessage>
//   Método: sendSearchRequest(FlightSearchRequest) → CompletableFuture<SendResult>
//   Log de sucesso/falha com MDC (correlationId)
//
// FlightSearchConsumer.java:
//   @KafkaListener(topics = "flight.search.requests", concurrency = "10")
//   Processa com Virtual Threads
//   Chama AmadeusClient.searchFlights()
//   Publica resultado em "flight.search.results"
//   Faz ACK manual após processamento bem-sucedido
//   Em erro: publica na DLT e faz NACK
//
// IMPORTANTE: o concurrency="10" + Virtual Threads garante
// capacidade para muitas requisições simultâneas
```

### 6. CONTROLLERS REST (com Swagger completo)

```java
// FlightSearchController.java
// Base path: /api/v1/flights
//
// GET /api/v1/flights/search
//   @Parameters: origin (req), destination (req), departureDate (req),
//                returnDate (opt), adults (default 1), cabinClass (opt)
//   @ApiResponse 200: List<FlightOfferResponse>
//   @ApiResponse 400: ValidationErrorResponse
//   @ApiResponse 503: AmadeusUnavailableResponse
//
// GET /api/v1/flights/history
//   @Parameters: origin, destination, days (default 30)
//   Retorna PriceHistoryResponse com trend e estatísticas
//
// GET /api/v1/flights/report
//   @Parameters: origin, destination, startDate, endDate
//   Retorna FlightReportResponse (min, max, avg, offers count)
//
// AlertController.java
// Base path: /api/v1/alerts
//
// POST /api/v1/alerts
//   @RequestBody: CreateAlertRequest (email, origin, dest, date, targetPrice)
//   @ApiResponse 201: AlertCreatedResponse
//
// GET /api/v1/alerts
//   @Parameters: email (required)
//   Retorna List<PriceAlertResponse>
//
// DELETE /api/v1/alerts/{id}
//   @ApiResponse 204: No Content
//
// PUT /api/v1/alerts/{id}/pause
// PUT /api/v1/alerts/{id}/resume
//
// TODOS os endpoints:
//   - @Validated com Bean Validation
//   - @Operation(summary=..., description=...) do OpenAPI
//   - @Tag(name="Flights") ou @Tag(name="Alerts")
//   - Retornam ResponseEntity com status codes correctos
//   - Têm @SecurityRequirement(name="bearerAuth") preparado
```

### 7. CONTROLLERS THYMELEAF (Frontend Web)

```java
// FlightWebController.java
// GET /  → redirige para /flights/search
// GET /flights/search → renderiza templates/flights/search.html
// POST /flights/search → processa busca, redirige para /flights/results
// GET /flights/results → renderiza resultados
// GET /flights/history → renderiza histórico de preços
//
// Template search.html deve ter:
//   - Formulário com campos: Origem (IATA), Destino (IATA), Data Ida,
//     Data Volta (opcional), Nº Adultos, Classe
//   - Sugestões hardcoded: BSB, OPO, LIS, GRU, GIG
//   - Tabela de resultados com ordenação por preço
//   - Gráfico de histórico (Chart.js via WebJar ou CDN)
//   - Indicador de variação de preço (seta verde/vermelha)
//   - Design: dark theme, cores azul/dourado, tipografia moderna
//   - Layout responsivo com Bootstrap 5 (WebJar)
//   - Componente de alertas em tempo real via SSE (Server-Sent Events)
```

### 8. SPRING BOOT ADMIN SERVER (módulo `flight-monitor-admin`)

```java
// AdminServerApplication.java
// @EnableAdminServer
// Porta: 8081
// Configurar:
//   - Autenticação básica (admin/admin em dev)
//   - Notificações: log quando instância cai/sobe
//   - Habilitar: /env endpoint para alterar variáveis em runtime
//   - Habilitar: /loggers para alterar log level em runtime
//   - Habilitar: /heapdump, /threaddump
//   - Interface customizada com nome "Flight Monitor Admin"
//   - CORS configurado para aceitar do app na porta 8080
//
// application.yml do admin:
//   spring.boot.admin.ui.title: "Flight Monitor — Admin"
//   management.endpoint.env.post.enabled: true
//   Actuator: expor todos os endpoints
```

### 9. DOCKER COMPOSE

```yaml
# docker/docker-compose.yml — gera com:
#
# services:
#   zookeeper:
#     image: confluentinc/cp-zookeeper:7.6.0
#     environment: ZOOKEEPER_CLIENT_PORT=2181
#
#   kafka:
#     image: confluentinc/cp-kafka:7.6.0
#     depends_on: zookeeper
#     ports: 9092:9092
#     environment:
#       KAFKA_BROKER_ID: 1
#       KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
#       KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
#       KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
#       KAFKA_AUTO_CREATE_TOPICS_ENABLE: true
#
#   kafka-ui:
#     image: provectuslabs/kafka-ui:latest
#     ports: 8090:8080
#     environment:
#       KAFKA_CLUSTERS_0_NAME: local
#       KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092
#
#   flight-monitor-app:
#     image: flight-monitor-app:latest
#     build: ../flight-monitor-app  (apenas para dev local)
#     ports: 8080:8080
#     environment:
#       SPRING_PROFILES_ACTIVE: prod
#       KAFKA_BOOTSTRAP_SERVERS: kafka:9092
#       AMADEUS_API_KEY: ${AMADEUS_API_KEY}
#       AMADEUS_API_SECRET: ${AMADEUS_API_SECRET}
#     depends_on: kafka
#
#   flight-monitor-admin:
#     image: flight-monitor-admin:latest
#     ports: 8081:8081
#     depends_on: flight-monitor-app
```

---

## TESTES UNITÁRIOS (mínimo 50% cobertura JaCoCo)

```java
// FlightSearchServiceTest.java — cobrir:
//   ✓ searchFlights() retorna lista ordenada por preço
//   ✓ searchFlights() usa cache em segunda chamada idêntica
//   ✓ searchFlights() lança exception para datas passadas
//   ✓ searchFlights() lança exception para IATA inválido (menos de 3 chars)
//   ✓ searchFlights() publica mensagem no Kafka (mock KafkaTemplate)
//   ✓ searchFlights() regista SearchLog com durationMs
//   ✓ checkAlerts() dispara PriceAlertMessage quando preço <= target
//   ✓ checkAlerts() não dispara quando preço > target
//   Usar: @ExtendWith(MockitoExtension.class), @Mock, @InjectMocks
//   Usar: assertThat() do AssertJ
//
// FlightSearchControllerTest.java — cobrir:
//   ✓ GET /api/v1/flights/search retorna 200 com lista
//   ✓ GET /api/v1/flights/search retorna 400 para origem vazia
//   ✓ GET /api/v1/flights/search retorna 400 para data passada
//   ✓ POST /api/v1/alerts retorna 201 com location header
//   ✓ DELETE /api/v1/alerts/{id} retorna 204
//   ✓ DELETE /api/v1/alerts/{id} retorna 404 para ID inexistente
//   Usar: @WebMvcTest, MockMvc, @MockBean
//
// FlightSearchProducerTest.java — cobrir:
//   ✓ sendSearchRequest() chama kafkaTemplate.send() com tópico correcto
//   ✓ sendSearchRequest() inclui correlationId no header da mensagem
//   ✓ sendSearchRequest() loga erro quando send() falha
//
// FlightSearchConsumerTest.java — cobrir:
//   ✓ consume() chama AmadeusClient com parâmetros correctos
//   ✓ consume() publica resultado em "flight.search.results"
//   ✓ consume() publica em DLT quando AmadeusClient lança exception
//   ✓ consume() faz ACK manual após sucesso
//
// AlertServiceTest.java — cobrir:
//   ✓ createAlert() persiste PriceAlert com status ACTIVE
//   ✓ createAlert() lança exception para email inválido
//   ✓ pauseAlert() muda status para PAUSED
//   ✓ checkAlerts() actualiza lastCheckedAt em todos os alertas activos
//
// PriceHistoryServiceTest.java — cobrir:
//   ✓ getHistory() retorna lista ordenada por recordedAt DESC
//   ✓ getHistory() calcula trend correctamente (UP/DOWN/STABLE)
//   ✓ saveSnapshot() cria PriceHistory com min/max/avg correctos

// JaCoCo config no POM:
// <rule>
//   <element>BUNDLE</element>
//   <limits>
//     <limit>
//       <counter>LINE</counter>
//       <value>COVEREDRATIO</value>
//       <minimum>0.50</minimum>
//     </limit>
//   </limits>
// </rule>
// Excluir: **/*Application.java, **/config/**, **/entity/**, **/dto/**
```

---

## TESTES DE SMOKE/INTEGRAÇÃO (pós-deploy)

```java
// FlightSearchSmokeTest.java
// @Tag("smoke")
// @SpringBootTest(webEnvironment = RANDOM_PORT)
// Usa REST Assured
//
// Testes:
//   ✓ GET /actuator/health retorna 200 e status UP
//   ✓ GET /actuator/info retorna 200 com versão da app
//   ✓ GET /swagger-ui.html retorna 200
//   ✓ GET /api-docs retorna 200 com JSON OpenAPI válido
//   ✓ GET /api/v1/flights/search?origin=BSB&destination=OPO&departureDate=NEXT_MONTH
//      retorna 200 (mesmo que lista vazia — valida que o pipeline funciona)
//   ✓ GET /h2-console retorna 200 (apenas em profile dev)
//
// KafkaSmokeTest.java
// @Tag("smoke")
// @SpringBootTest
// @EmbeddedKafka(partitions = 1, topics = {"flight.search.requests", ...})
//
// Testes:
//   ✓ Producer envia mensagem → Consumer processa → resultado em "flight.search.results"
//   ✓ Latência end-to-end < 5000ms para processamento de mensagem
//   ✓ DLT recebe mensagem quando Consumer lança RuntimeException
//
// AlertSmokeTest.java
// @Tag("smoke")
//
// Testes:
//   ✓ Fluxo completo: criar alerta → simular queda de preço → alerta disparado
//   ✓ GET /api/v1/alerts?email=test@test.com retorna lista (pode estar vazia)
//   ✓ POST /api/v1/alerts → DELETE /api/v1/alerts/{id} → alerta removido

// application-test.yml:
// Configurar H2 in-memory puro para testes
// Kafka: spring.embedded.kafka=true
// amadeus.api.key=test-key (usa mock do AmadeusClient)
// Desactivar Spring Boot Admin client em testes
```

---

## GITHUB ACTIONS — PIPELINE COMPLETA

### `.github/workflows/ci.yml`

```yaml
# Nome: CI — Build, Test & Quality
# Trigger: push em qualquer branch, PR para main
#
# Jobs:
#
# 1. build-and-test:
#    runs-on: ubuntu-latest
#    services:
#      kafka:
#        image: confluentinc/cp-kafka:7.6.0
#        ports: 9092:9092
#        env: KAFKA_ZOOKEEPER_CONNECT, etc.
#      zookeeper:
#        image: confluentinc/cp-zookeeper:7.6.0
#    steps:
#      - checkout
#      - Setup Java 21 (temurin)
#      - Cache Maven ~/.m2
#      - mvn verify -P unit-tests
#      - Upload JaCoCo report como artefacto
#      - Upload Surefire reports como artefacto
#      - Falha se cobertura < 50% (enforced pelo JaCoCo)
#
# 2. code-quality:
#    needs: build-and-test
#    steps:
#      - SonarCloud scan (se SONAR_TOKEN configurado)
#      - OWASP Dependency Check
#      - Upload vulnerability report
#
# 3. build-docker-image:
#    needs: build-and-test
#    if: branch == 'main' ou 'develop'
#    steps:
#      - mvn spring-boot:build-image -DskipTests
#        -Dspring-boot.build-image.imageName=ghcr.io/${{ github.repository }}/flight-monitor:${{ github.sha }}
#      - Login no ghcr.io
#      - docker push
#      - Tag também como 'latest' se branch == 'main'
```

### `.github/workflows/cd.yml`

```yaml
# Nome: CD — Deploy
# Trigger: push em main (após CI verde) ou manual dispatch
#
# Jobs:
#
# 1. deploy-staging:
#    environment: staging
#    steps:
#      - Pull da imagem Docker do ghcr.io
#      - Deploy via docker-compose ou kubectl (parametrizado)
#      - Health check: curl -f http://staging-host/actuator/health
#      - Aguarda 30s para startup
#
# 2. smoke-tests-staging:
#    needs: deploy-staging
#    steps:
#      - checkout
#      - Setup Java 21
#      - mvn verify -P smoke-tests -Dapp.base.url=http://staging-host
#      - Upload relatório de smoke tests
#      - Falha o workflow se algum smoke test falhar
#
# 3. deploy-production:
#    needs: smoke-tests-staging
#    environment: production (requires manual approval)
#    steps:
#      - Deploy em produção
#      - Health check de produção
#      - Notificação de sucesso
```

### `.github/workflows/smoke-tests.yml`

```yaml
# Nome: Smoke Tests — Post Deploy
# Trigger: workflow_dispatch com input app_url
#          repository_dispatch com event-type=post-deploy
#
# Permite ser chamado após qualquer deploy externo
# steps:
#   - checkout
#   - Java 21 setup
#   - mvn test -P smoke-only -Dapp.base.url=${{ inputs.app_url }}
#   - Se falha: cria GitHub Issue automaticamente com o relatório
#   - Notifica Slack/Teams (se webhook configurado)
```

---

## DOCUMENTAÇÃO ZNAI

Gera os seguintes ficheiros de documentação em `docs/content/`:

### `introduction.md`
```markdown
# Deve conter:
# - O que é o Flight Price Monitor
# - Diagrama de alto nível da arquitectura (ASCII art ou tabela)
# - Quick Start: como correr em 3 comandos (clone, docker-compose up, aceder à app)
# - Links para: Swagger UI, Admin Dashboard, H2 Console, Kafka UI
# - Tabela de portas: 8080 (app), 8081 (admin), 8090 (kafka-ui), 9092 (kafka)
```

### `architecture.md`
```markdown
# Deve conter:
# - Diagrama de componentes (pode ser mermaid ou texto estruturado para Znai)
# - Descrição de cada módulo Maven
# - Decisões de arquitectura e justificações:
#   * Por que Virtual Threads para chamadas Amadeus
#   * Por que H2 file-mode em dev vs produção
#   * Por que Kafka para desacoplar pesquisas de alta concorrência
#   * Por que módulo separado para Admin
# - Variáveis de ambiente e os seus defaults
```

### `layers-communication.md`
```markdown
# Deve conter o fluxo completo de uma pesquisa de voos:
#
# 1. HTTP Request → FlightWebController (ou FlightSearchController REST)
# 2. Controller valida Request com @Validated
# 3. Controller chama FlightSearchService.searchFlights()
# 4. Service verifica Cache → HIT: retorna imediato / MISS: continua
# 5. Service cria FlightSearchRequestMessage
# 6. FlightSearchProducer.send() → Kafka tópico "flight.search.requests"
# 7. FlightSearchConsumer recebe (concurrency=10, Virtual Threads)
# 8. Consumer chama AmadeusClient.searchFlights()
# 9. Consumer publica FlightSearchResultMessage em "flight.search.results"
# 10. Service recebe resultado via CompletableFuture.get(30s)
# 11. Service persiste FlightOffer entities via Repository
# 12. Service salva PriceHistory snapshot
# 13. Service chama AlertService.checkAlertsForRoute()
# 14. AlertService publica PriceAlertMessage se threshold atingido
# 15. Service actualiza Cache
# 16. Service converte para DTO via Mapper
# 17. Controller retorna ResponseEntity<List<FlightOfferResponse>>
# 18. Thymeleaf renderiza results.html (ou JSON para REST)
#
# Incluir tabela de responsabilidades por camada
# Incluir regras de o que NÃO deve estar em cada camada
```

### `messaging.md`
```markdown
# Deve conter:
# - Topologia dos tópicos Kafka e suas funções
# - Schema das mensagens (FlightSearchRequestMessage, PriceAlertMessage)
# - Estratégia de Error Handling: Retry → DLT
# - Como escalar: aumentar partições e concurrency para alta carga
# - Monitorização: métricas Kafka disponíveis via Actuator
# - Como testar localmente com Kafka UI (http://localhost:8090)
```

### `adding-new-feature.md`
```markdown
# Guia completo para adicionar uma nova feature. Exemplo: "Alertas de bagagem"
#
# Passos obrigatórios (checklist):
# [ ] 1. Criar a Entity em domain/entity/ com @Entity e @Table
# [ ] 2. Criar o Repository extends JpaRepository
# [ ] 3. Criar os DTOs em dto/request/ e dto/response/
# [ ] 4. Criar o Mapper (interface ou classe com métodos toEntity/toResponse)
# [ ] 5. Criar a Service interface em service/
# [ ] 6. Criar a ServiceImpl em service/ com lógica de negócio
# [ ] 7. Se assíncrono: criar Message DTO em messaging/dto/
# [ ] 8. Se assíncrono: criar Producer e Consumer em messaging/
# [ ] 9. Criar o Controller REST em controller/ com anotações OpenAPI
# [ ] 10. Criar template Thymeleaf em resources/templates/
# [ ] 11. Registar novo tópico Kafka em KafkaConfig se necessário
# [ ] 12. Escrever testes unitários (mínimo: service + controller)
# [ ] 13. Escrever pelo menos 1 smoke test end-to-end
# [ ] 14. Actualizar esta documentação
# [ ] 15. Actualizar CHANGELOG.md
#
# Exemplo de implementação de cada ficheiro para "BaggageAlertFeature"
# com código real (não placeholder)
```

### `deployment.md`
```markdown
# Deve conter:
# - Como gerar a imagem Docker: mvn spring-boot:build-image
# - Como publicar no GHCR
# - Como correr com Docker Compose
# - Como configurar variáveis de ambiente em produção
# - Como usar o Spring Boot Admin para:
#   * Alterar log level em runtime
#   * Alterar variável de ambiente via /env endpoint
#   * Ver thread dump e heap dump
#   * Monitorizar health e métricas
# - Pipeline CI/CD: como fazer deploy manual via workflow_dispatch
# - Rollback: como reverter para versão anterior
```

---

## CONFIGURAÇÕES ADICIONAIS OBRIGATÓRIAS

### Maven Profiles

```xml
<!-- Adicionar ao POM do módulo app: -->

<!-- Profile "unit-tests": -->
<!--   surefire executa apenas *Test.java (sem @Tag("smoke")) -->
<!--   jacoco prepare-agent + report + check (50%) -->

<!-- Profile "smoke-tests": -->
<!--   failsafe executa *SmokeTest.java (@Tag("smoke")) -->
<!--   -Dapp.base.url configurável -->
<!--   surefire skipTests=true (não re-executa unitários) -->

<!-- Profile "smoke-only": -->
<!--   Igual a smoke-tests mas sem build completo -->
<!--   Para usar no workflow smoke-tests.yml -->
```

### Spring Boot Admin — Funcionalidades a habilitar

```yaml
# No módulo admin, application.yml:
# - Permitir POST em /env para alterar variáveis sem restart
# - Permitir POST em /loggers para alterar nível de log
# - Exibir: heap memory, CPU, threads ativos, GC stats
# - Configurar wallboard view como página inicial
# - Notificações: logar no console quando instância fica DOWN
# - Configurar retention de eventos: 100 eventos
```

### Znai Configuration

```
# docs/znai.config:
# docId: flight-monitor
# title: Flight Price Monitor
# type: User Guide
#
# Estrutura de navegação:
# - Getting Started
#   - Introduction
#   - Architecture
# - Developer Guide
#   - Layers Communication
#   - Messaging
#   - Adding New Features
# - Operations
#   - Deployment
#   - API Reference
```

---

## REGRAS DE GERAÇÃO DE CÓDIGO

1. **Nenhum `TODO` ou `FIXME`** no código gerado — implementa completo
2. **Sem código morto** — todos os métodos declarados devem ser implementados
3. **Logging** com SLF4J em todos os serviços: INFO para fluxo normal, WARN para retries, ERROR para falhas
4. **MDC** com `correlationId` propagado do Controller até ao Consumer Kafka
5. **Validação** com Bean Validation (`@NotBlank`, `@Pattern(regexp="[A-Z]{3}")` para IATA, `@Future` para datas)
6. **Tratamento de erros** consistente: `GlobalExceptionHandler` retorna `ProblemDetail` (RFC 9457, Spring 6)
7. **Imports** organizados: sem wildcard imports
8. **Records** Java 21 para DTOs imutáveis
9. **Sealed classes** para tipos de resposta onde aplicável
10. **Javadoc** nos métodos públicos de Service e Controller
11. **`@Transactional`** apenas nos métodos de escrita dos Services
12. **Profiles** bem separados: dev usa H2 file, test usa H2 in-memory, prod não usa H2

---

## RESULTADO ESPERADO

Após executar este prompt, o GitHub Copilot deve gerar um projecto que:

- **Compila** com `mvn clean compile` sem erros
- **Testa** com `mvn verify -P unit-tests` com cobertura ≥ 50%
- **Constrói imagem Docker** com `mvn spring-boot:build-image`
- **Corre localmente** com `docker-compose up` em `docker/`
- **Documenta** via Znai em `docs/`
- **Pipeline CI/CD** funciona ao fazer push no GitHub

URLs após `docker-compose up`:
- `http://localhost:8080` — Aplicação principal
- `http://localhost:8080/swagger-ui.html` — API Docs
- `http://localhost:8080/h2-console` — Database Console
- `http://localhost:8081` — Spring Boot Admin
- `http://localhost:8090` — Kafka UI

---

*Fim do prompt. Cole tudo acima no GitHub Copilot Chat em modo Agent.*
