# TASK — Messaging Abstraction Layer
# Flight Price Monitor · GitHub Copilot Agent
# ─────────────────────────────────────────────
# Cole esta task no GitHub Copilot Chat (@workspace, modo Agent)
# ou abra como Copilot Issue Task no repositório.
#
# OBJETIVO: substituir a dependência directa do Kafka por uma camada
# de abstracção que permita alternar entre Kafka, RabbitMQ, Apache Pulsar
# e modo SYNC (sem broker — para deploy em ambientes free/CI).
# Activação por Spring Profile: kafka | rabbitmq | pulsar | sync
# ─────────────────────────────────────────────

## CONTEXTO DO PROJECTO

Repositório: fidelisfelipe/project-test-ai
Módulo alvo: flight-monitor-app
Base package: com.flightmonitor
Java: 17 (manter compatibilidade, sem Virtual Threads por agora)
Spring Boot: 3.3.4

Problema actual:
- FlightSearchProducer e FlightSearchConsumer dependem directamente
  de KafkaTemplate e @KafkaListener
- Sem Kafka a correr, o contexto Spring falha no arranque
- Impossível deployar em ambientes gratuitos (Render, Koyeb) sem broker

Solução pretendida:
- Padrão Strategy: uma interface MessageBus com implementações
  por broker, seleccionadas via @Profile
- Profile "sync": execução síncrona directa sem qualquer broker
- Profiles "kafka", "rabbitmq", "pulsar": implementações reais
- Zero alteração nos Services — só a camada de messaging muda

---

## PADRÃO ARQUITECTURAL A IMPLEMENTAR

```
FlightSearchService
      │
      ▼
  MessageBus (interface)          ← novo contrato único
      │
      ├── SyncMessageBus          (@Profile("sync")     — sem broker)
      ├── KafkaMessageBus         (@Profile("kafka")    — Kafka existente)
      ├── RabbitMQMessageBus      (@Profile("rabbitmq") — RabbitMQ)
      └── PulsarMessageBus        (@Profile("pulsar")   — Apache Pulsar)
```

---

## FICHEIROS A CRIAR / MODIFICAR

```
flight-monitor-app/src/main/java/com/flightmonitor/
├── messaging/
│   ├── MessageBus.java                        ← CRIAR (interface)
│   ├── MessageHandler.java                    ← CRIAR (interface callback)
│   ├── MessageBusFactory.java                 ← CRIAR
│   ├── sync/
│   │   └── SyncMessageBus.java                ← CRIAR
│   ├── kafka/
│   │   └── KafkaMessageBus.java               ← CRIAR (refactor do existente)
│   ├── rabbitmq/
│   │   └── RabbitMQMessageBus.java            ← CRIAR
│   └── pulsar/
│       └── PulsarMessageBus.java              ← CRIAR
├── config/
│   ├── MessagingConfig.java                   ← CRIAR
│   ├── KafkaConfig.java                       ← MODIFICAR (condicional)
│   ├── RabbitMQConfig.java                    ← CRIAR
│   └── PulsarConfig.java                      ← CRIAR
└── client/
    └── AmadeusClientMock.java                 ← CRIAR (para profile sync)

flight-monitor-app/src/main/resources/
├── application.yml                            ← MODIFICAR
├── application-sync.yml                       ← CRIAR
├── application-kafka.yml                      ← CRIAR (mover config Kafka)
├── application-rabbitmq.yml                   ← CRIAR
└── application-pulsar.yml                     ← CRIAR

flight-monitor-app/pom.xml                     ← MODIFICAR (deps condicionais)

flight-monitor-app/src/test/
└── unit/messaging/
    ├── SyncMessageBusTest.java                ← CRIAR
    ├── MessageBusFactoryTest.java             ← CRIAR
    └── KafkaMessageBusTest.java               ← MODIFICAR
```

---

## ESPECIFICAÇÕES DETALHADAS

### 1. Interface `MessageBus`

```java
// MessageBus.java
// package com.flightmonitor.messaging
//
// Interface central de envio de mensagens.
// Todas as implementações devem ser thread-safe.
//
// public interface MessageBus {
//
//   /**
//    * Sends a flight search request message.
//    * In sync mode: processes immediately in the same thread.
//    * In async mode: publishes to the configured broker.
//    *
//    * @param message the search request
//    * @return CompletableFuture that completes when the message is accepted
//    *         (sync: when processing finishes; async: when broker confirms)
//    */
//   CompletableFuture<MessageSendResult> sendSearchRequest(
//       FlightSearchRequestMessage message);
//
//   /**
//    * Sends a price alert notification message.
//    */
//   CompletableFuture<MessageSendResult> sendPriceAlert(
//       PriceAlertMessage message);
//
//   /**
//    * Returns the active broker type for health checks and admin display.
//    */
//   BrokerType getBrokerType();
//
//   /**
//    * Checks if the broker is reachable.
//    * Sync always returns true.
//    */
//   boolean isAvailable();
// }
//
// BrokerType enum: SYNC, KAFKA, RABBITMQ, PULSAR
//
// MessageSendResult record:
//   boolean success
//   String brokerType        // "SYNC" | "KAFKA" | "RABBITMQ" | "PULSAR"
//   String messageId         // UUID ou offset/sequence do broker
//   long processingTimeMs
//   String errorMessage      // nullable
//   static MessageSendResult ok(BrokerType, String messageId, long ms)
//   static MessageSendResult failed(BrokerType, String error)
```

### 2. Interface `MessageHandler`

```java
// MessageHandler.java
// Callback usado pelo SyncMessageBus e pelos consumers reais
// para processar mensagens recebidas.
//
// public interface MessageHandler {
//   FlightSearchResultMessage handleSearchRequest(
//       FlightSearchRequestMessage message) throws Exception;
//
//   void handlePriceAlert(PriceAlertMessage message) throws Exception;
// }
//
// @Component DefaultMessageHandler implements MessageHandler:
//   Injecta AmadeusClient e FlightOfferMapper
//   handleSearchRequest(): chama amadeusClient.searchFlights(),
//     constrói FlightSearchResultMessage e retorna
//   handlePriceAlert(): loga o alerta (em prod: enviaria email)
```

### 3. `SyncMessageBus` — implementação para deploy free/CI

```java
// @Component @Profile("sync")
// public class SyncMessageBus implements MessageBus {
//
//   // Injectar: MessageHandler, SearchLogRepository
//
//   // sendSearchRequest(message):
//   //   val start = System.currentTimeMillis()
//   //   MDC.put("correlationId", message.correlationId())
//   //   try {
//   //     log.info("[SYNC][{}] Processing search {} -> {} directly",
//   //              message.correlationId(), message.origin(), message.destination())
//   //     val result = messageHandler.handleSearchRequest(message)
//   //     log.info("[SYNC][{}] Completed in {}ms — {} offers",
//   //              message.correlationId(),
//   //              System.currentTimeMillis() - start,
//   //              result.offers().size())
//   //     return CompletableFuture.completedFuture(
//   //       MessageSendResult.ok(SYNC, message.correlationId(),
//   //                            System.currentTimeMillis() - start))
//   //   } catch (Exception ex) {
//   //     log.error("[SYNC][{}] Error: {}", message.correlationId(), ex.getMessage())
//   //     return CompletableFuture.completedFuture(
//   //       MessageSendResult.failed(SYNC, ex.getMessage()))
//   //   } finally {
//   //     MDC.remove("correlationId")
//   //   }
//
//   // sendPriceAlert(message):
//   //   tenta messageHandler.handlePriceAlert(message)
//   //   em caso de erro: loga WARN e retorna failed (best-effort)
//   //   sempre retorna CompletableFuture completado (nunca pendente)
//
//   // getBrokerType(): return BrokerType.SYNC
//   // isAvailable(): return true  (sempre disponível)
// }
```

### 4. `KafkaMessageBus` — refactor do código existente

```java
// @Component @Profile("kafka")
// public class KafkaMessageBus implements MessageBus {
//
//   // Mover lógica de FlightSearchProducer e PriceAlertProducer
//   // para cá. FlightSearchProducer e PriceAlertProducer passam a
//   // ser @Deprecated mas mantidos por compatibilidade por 1 versão.
//
//   // Injectar: KafkaTemplate<String, FlightSearchRequestMessage>,
//   //           KafkaTemplate<String, PriceAlertMessage>
//
//   // sendSearchRequest():
//   //   kafkaTemplate.send("flight.search.requests",
//   //                       message.correlationId(), message)
//   //   trata CompletableFuture do KafkaTemplate
//   //   retorna MessageSendResult.ok() com offset como messageId
//
//   // sendPriceAlert():
//   //   kafkaTemplate.send("price.alerts", ...)
//
//   // isAvailable():
//   //   tenta listagem de tópicos via AdminClient com timeout 2s
//   //   retorna false em caso de TimeoutException
// }
//
// KafkaConfig.java — MODIFICAR:
//   Envolver todos os @Bean em @ConditionalOnProfile("kafka")
//   Beans de tópicos (NewTopic) só criam se profile=kafka activo
//   @KafkaListener nos consumers: adicionar
//     @ConditionalOnProperty(name="spring.kafka.enabled", havingValue="true")
//     OU simplesmente usar @Profile("kafka") na classe consumer
```

### 5. `RabbitMQMessageBus`

```java
// @Component @Profile("rabbitmq")
// public class RabbitMQMessageBus implements MessageBus {
//
//   // Injectar: RabbitTemplate, MessageHandler
//   //           (RabbitTemplate auto-configurado pelo Spring Boot)
//
//   // sendSearchRequest():
//   //   rabbitTemplate.convertAndSend(
//   //     "flight.exchange",           // exchange
//   //     "flight.search.requests",    // routing key
//   //     message)
//   //   em caso de AmqpException: retorna MessageSendResult.failed()
//
//   // sendPriceAlert():
//   //   rabbitTemplate.convertAndSend("flight.exchange", "price.alerts", message)
//
//   // isAvailable():
//   //   rabbitTemplate.execute(channel -> channel.isOpen())
//   //   em caso de AmqpException: retorna false
//
//   // getBrokerType(): return BrokerType.RABBITMQ
// }
//
// RabbitMQConfig.java (@Configuration @Profile("rabbitmq")):
//   @Bean TopicExchange flightExchange("flight.exchange", durable=true, autoDelete=false)
//   @Bean Queue searchRequestsQueue("flight.search.requests", durable=true)
//   @Bean Queue priceAlertsQueue("price.alerts", durable=true)
//   @Bean Binding searchBinding(queue, exchange, routingKey="flight.search.requests")
//   @Bean Binding alertsBinding(queue, exchange, routingKey="price.alerts")
//   @Bean MessageConverter jackson2JsonMessageConverter()
//
// Consumer RabbitMQ (inner class ou classe separada):
//   @RabbitListener(queues="flight.search.requests")
//   @Profile("rabbitmq")
//   void consume(FlightSearchRequestMessage message):
//     messageHandler.handleSearchRequest(message)
//     (sem ACK manual — RabbitMQ auto-ack por default para simplificar)
```

### 6. `PulsarMessageBus`

```java
// @Component @Profile("pulsar")
// public class PulsarMessageBus implements MessageBus {
//
//   // Usar spring-pulsar (spring-boot-starter-pulsar, incluído no BOM 3.3.x)
//   // Injectar: PulsarTemplate<FlightSearchRequestMessage>,
//   //           PulsarTemplate<PriceAlertMessage>
//
//   // sendSearchRequest():
//   //   pulsarTemplate.send("persistent://public/default/flight-search-requests",
//   //                        message)
//   //   em caso de PulsarClientException: retorna failed()
//
//   // sendPriceAlert():
//   //   pulsarTemplate.send("persistent://public/default/price-alerts", message)
//
//   // isAvailable():
//   //   tenta PulsarClient.getServiceUrl() com timeout 2s
//
//   // getBrokerType(): return BrokerType.PULSAR
// }
//
// PulsarConfig.java (@Configuration @Profile("pulsar")):
//   Configura PulsarClient com serviceUrl do application-pulsar.yml
//   @Bean PulsarConsumerFactory para os tópicos
//
// Consumer Pulsar:
//   @PulsarListener(topics="persistent://public/default/flight-search-requests",
//                   subscriptionName="flight-monitor-sub")
//   @Profile("pulsar")
//   void consume(FlightSearchRequestMessage message):
//     messageHandler.handleSearchRequest(message)
```

### 7. `AmadeusClientMock` — para profile sync em ambientes free

```java
// @Component @Profile("sync")  (ou @Primary @Profile("sync"))
// public class AmadeusClientMock implements AmadeusClient {
//
//   // Dados mock realistas para a rota BSB → OPO
//   // Retorna sempre a mesma lista de 5 ofertas simuladas com:
//   //   - Preços realistas: €651–€898
//   //   - Companhias: LATAM, TAP, Iberia, Air France, KLM
//   //   - Escalas: 1–2
//   //   - Datas: baseadas na data do request
//
//   // List<FlightOffer> searchFlights(FlightSearchRequest request):
//   //   log.info("[MOCK] Returning simulated offers for {} -> {}",
//   //            request.origin(), request.destination())
//   //   simula delay de 300ms (Thread.sleep) para parecer realista
//   //   constrói 5 FlightOffer entities com dados hardcoded
//   //   adiciona variação aleatória de ±5% no preço a cada chamada
//   //   retorna lista ordenada por preço
//
//   // boolean isAvailable(): return true
//
//   // Dados mock (constantes da classe):
//   //   List<String[]> MOCK_AIRLINES = [
//   //     ["LA", "LATAM Airlines", "1", "22h15m"],
//   //     ["TP", "TAP Air Portugal", "1", "16h40m"],
//   //     ["IB", "Iberia", "1", "18h20m"],
//   //     ["AF", "Air France", "1", "17h50m"],
//   //     ["KL", "KLM", "2", "24h10m"]
//   //   ]
//   //   double[] BASE_PRICES = {651.0, 742.0, 798.0, 831.0, 589.0}
```

### 8. `MessagingConfig` — selecção automática

```java
// @Configuration
// public class MessagingConfig {
//
//   // @Bean @ConditionalOnMissingBean(MessageBus.class)
//   // MessageBus fallbackMessageBus():
//   //   log.warn("No MessageBus profile active — falling back to SYNC mode")
//   //   return new SyncMessageBus(...)
//   //   (garante que a app arranca mesmo sem profile explícito)
//
//   // @Bean MessageBusHealthIndicator(MessageBus bus):
//   //   Expõe /actuator/health com status do broker activo
//   //   UP se bus.isAvailable(), DOWN caso contrário
//   //   Inclui detalhe: brokerType, available
// }
```

### 9. `FlightSearchServiceImpl` — remover dependência directa de Kafka

```java
// MODIFICAR FlightSearchServiceImpl:
//
// Substituir:
//   @Autowired FlightSearchProducer producer
//   @Autowired PriceAlertProducer alertProducer
//
// Por:
//   @Autowired MessageBus messageBus
//
// No método searchFlights():
//   REMOVER: producer.sendSearchRequest(request)
//   REMOVER: CompletableFuture.get(30s) do resultado Kafka
//
//   ADICIONAR:
//   val message = new FlightSearchRequestMessage(...)
//   val sendResult = messageBus.sendSearchRequest(message).get(30, SECONDS)
//   if (!sendResult.success()) {
//     throw new AmadeusApiException("Messaging failed: " + sendResult.errorMessage(),
//                                    503, "MSG_FAILURE", null)
//   }
//   // Em modo sync: o resultado já está em sendResult (via MessageHandler)
//   // Em modo async: aguardar resultado via CompletableFuture separado
//   //   (o Consumer publica em "flight.search.results" como antes)
//
// NOTA: manter backward compatibility — se FlightSearchProducer
// ainda existir como bean (profile kafka), o service usa MessageBus
// que internamente usa o producer. Zero dupplicação.
```

### 10. Profiles YAML

```yaml
# application-sync.yml
spring:
  profiles:
    active: sync
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
      - org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration
  kafka:
    enabled: false
amadeus:
  api:
    key: mock
    secret: mock
flight:
  monitor:
    mock-mode: true

# application-kafka.yml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
      properties:
        enable.idempotence: true
    consumer:
      group-id: flight-monitor-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.flightmonitor.*"
    listener:
      concurrency: 10
      ack-mode: MANUAL_IMMEDIATE

# application-rabbitmq.yml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USER:guest}
    password: ${RABBITMQ_PASSWORD:guest}
    virtual-host: /
    connection-timeout: 5s
    listener:
      simple:
        concurrency: 5
        max-concurrency: 10
        acknowledge-mode: auto
        retry:
          enabled: true
          max-attempts: 3
          initial-interval: 1s
          multiplier: 2.0
          max-interval: 4s

# application-pulsar.yml
spring:
  pulsar:
    client:
      service-url: ${PULSAR_SERVICE_URL:pulsar://localhost:6650}
    producer:
      send-timeout: 30s
    consumer:
      subscription-initial-position: earliest
```

### 11. Modificar `pom.xml` — dependências condicionais

```xml
<!-- Adicionar ao pom.xml do flight-monitor-app:
     As dependências de RabbitMQ e Pulsar são opcionais —
     só activas com os profiles correspondentes.
     Spring Boot auto-configura se as libs estiverem no classpath
     E o profile estiver activo. -->

<!-- RabbitMQ — incluir sempre (leve, só activa se profile=rabbitmq) -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>

<!-- Pulsar — incluir sempre (Spring Boot BOM 3.3.x gere versão) -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-pulsar</artifactId>
</dependency>

<!-- Manter spring-kafka já existente -->

<!-- NOTA: Não usar <optional>true</optional> nas dependências de broker
     porque os @Profile já garantem que os beans não são criados.
     Manter todas no classpath simplifica o build. -->
```

### 12. Dockerfile para deploy no Render.com

```dockerfile
# Dockerfile — na raiz do projecto (flight-price-monitor/)
# Multi-stage build: compila com Maven, executa com JRE slim

FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY flight-monitor-app/pom.xml flight-monitor-app/
COPY flight-monitor-admin/pom.xml flight-monitor-admin/

# Download dependências (layer cache)
RUN mvn dependency:go-offline -B -pl flight-monitor-app \
    --no-transfer-progress 2>/dev/null || true

COPY flight-monitor-app/src flight-monitor-app/src
COPY flight-monitor-admin/src flight-monitor-admin/src

# Build sem testes (testes correm no CI)
RUN mvn package -pl flight-monitor-app -am \
    -DskipTests --no-transfer-progress

FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# Utilizador não-root por segurança
RUN addgroup -S flightmonitor && adduser -S flightmonitor -G flightmonitor
USER flightmonitor

COPY --from=build /app/flight-monitor-app/target/*.jar app.jar

# Profile sync por default — sem broker necessário
ENV SPRING_PROFILES_ACTIVE=sync
ENV SERVER_PORT=8080
ENV JAVA_OPTS="-Xms128m -Xmx400m -XX:+UseContainerSupport"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### 13. `render.yaml` — deploy automático no Render.com

```yaml
# render.yaml — na raiz do projecto
# Usado pelo Render para configurar o serviço automaticamente

services:
  - type: web
    name: flight-monitor-app
    runtime: docker
    dockerfilePath: ./Dockerfile
    plan: free
    region: frankfurt
    branch: main
    healthCheckPath: /actuator/health
    envVars:
      - key: SPRING_PROFILES_ACTIVE
        value: sync
      - key: AMADEUS_API_KEY
        sync: false   # preencher manualmente no dashboard Render
      - key: AMADEUS_API_SECRET
        sync: false
      - key: SERVER_PORT
        value: "8080"
    autoDeploy: true
```

---

## TESTES A CRIAR / MODIFICAR

```java
// SyncMessageBusTest.java
// @ExtendWith(MockitoExtension.class)
// @Mock: MessageHandler
// @InjectMocks: SyncMessageBus
//
// @Test
// should_CompleteImmediately_When_SyncModeActive():
//   given(messageHandler.handleSearchRequest(any())).willReturn(mockResult())
//   val future = bus.sendSearchRequest(testMessage())
//   assertThat(future).isDone()   // já completo — não pendente
//   assertThat(future.get().success()).isTrue()
//   assertThat(future.get().brokerType()).isEqualTo("SYNC")
//
// @Test
// should_ReturnFailed_When_HandlerThrowsException():
//   given(messageHandler.handleSearchRequest(any()))
//     .willThrow(new AmadeusApiException(...))
//   val future = bus.sendSearchRequest(testMessage())
//   assertThat(future).isDone()
//   assertThat(future.get().success()).isFalse()
//   assertThat(future.get().errorMessage()).isNotBlank()
//
// @Test
// should_AlwaysBeAvailable_InSyncMode():
//   assertThat(bus.isAvailable()).isTrue()
//
// @Test
// should_PropagateMDCCorrelationId_WhenProcessing():
//   MDC.put("correlationId", "test-123")
//   given(messageHandler.handleSearchRequest(any())).willAnswer(inv -> {
//     assertThat(MDC.get("correlationId")).isEqualTo("test-123")
//     return mockResult()
//   })
//   bus.sendSearchRequest(testMessage())
//
// MessageBusFactoryTest.java
// @SpringBootTest @ActiveProfiles("sync")
// @Test void sync_profile_loads_SyncMessageBus():
//   @Autowired MessageBus bus
//   assertThat(bus).isInstanceOf(SyncMessageBus.class)
//   assertThat(bus.getBrokerType()).isEqualTo(BrokerType.SYNC)
//   assertThat(bus.isAvailable()).isTrue()
//
// KafkaMessageBusTest.java — MODIFICAR para usar KafkaMessageBus
// em vez de FlightSearchProducer directamente
```

---

## CRITÉRIOS DE CONCLUSÃO

```bash
# 1. Compila sem erros com todos os profiles
mvn clean compile -pl flight-monitor-app

# 2. App arranca com profile sync SEM Kafka, RabbitMQ ou Pulsar
mvn spring-boot:run -pl flight-monitor-app \
  -Dspring-boot.run.arguments="--spring.profiles.active=sync" &
sleep 20
curl -f http://localhost:8080/actuator/health
# Esperado: {"status":"UP","components":{"messageBus":{"status":"UP","details":{"brokerType":"SYNC"}}}}
kill %1

# 3. Pesquisa de voos funciona em modo sync (dados mock)
curl "http://localhost:8080/api/v1/flights/search\
?origin=BSB&destination=OPO&departureDate=$(date -d '+3 months' +%Y-%m-%d)"
# Esperado: lista de 5 ofertas mock (LATAM, TAP, Iberia, Air France, KLM)

# 4. Testes unitários passam sem broker
mvn test -pl flight-monitor-app \
  -Dtest="SyncMessageBusTest,MessageBusFactoryTest" \
  -Dspring.profiles.active=sync

# 5. Testes de smoke passam com profile sync
mvn test -pl flight-monitor-app -P smoke-tests \
  -Dspring.profiles.active=sync \
  -DAMADEUS_API_KEY=test-key

# 6. Verifica que profile kafka ainda funciona (com EmbeddedKafka)
mvn test -pl flight-monitor-app \
  -Dtest="KafkaMessageBusTest" \
  -Dspring.profiles.active=kafka

# 7. Dockerfile builda correctamente
docker build -t flight-monitor-test .
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=sync \
  flight-monitor-test &
sleep 30
curl -f http://localhost:8080/actuator/health
docker stop $(docker ps -q --filter ancestor=flight-monitor-test)
```

---

## ESTADO ESPERADO APÓS ESTA TASK

✅ Interface MessageBus com 4 implementações (SYNC, Kafka, RabbitMQ, Pulsar)
✅ Profile "sync": app arranca sem qualquer broker, usa AmadeusClientMock
✅ Profile "kafka": comportamento exactamente igual ao anterior
✅ Profile "rabbitmq": consumer + producer RabbitMQ funcional
✅ Profile "pulsar": consumer + producer Pulsar funcional
✅ FlightSearchServiceImpl desacoplado — só conhece MessageBus
✅ Dockerfile multi-stage com profile sync por default
✅ render.yaml para deploy automático no Render.com
✅ Testes unitários do SyncMessageBus com cobertura de cenários de erro
✅ Health indicator em /actuator/health mostra broker activo

## ALTERNÂNCIA EM PRODUÇÃO

Para alternar entre brokers sem rebuild:
```bash
# Kafka (padrão local com docker-compose)
SPRING_PROFILES_ACTIVE=kafka docker-compose up

# RabbitMQ
SPRING_PROFILES_ACTIVE=rabbitmq \
RABBITMQ_HOST=my-rabbit.cloudamqp.com \
java -jar flight-monitor-app.jar

# Pulsar
SPRING_PROFILES_ACTIVE=pulsar \
PULSAR_SERVICE_URL=pulsar+ssl://my-pulsar.streamnative.io:6651 \
java -jar flight-monitor-app.jar

# Sem broker (Render free tier, CI, testes locais)
SPRING_PROFILES_ACTIVE=sync java -jar flight-monitor-app.jar
```
