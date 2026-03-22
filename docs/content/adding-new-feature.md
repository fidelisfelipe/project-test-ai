# Adding a New Feature

## Checklist

When adding a new feature to Flight Price Monitor, follow this checklist:

- [ ] Create domain entity in `domain/entity/` with JPA annotations
- [ ] Add enum in `domain/enums/` if needed
- [ ] Create repository interface extending `JpaRepository`
- [ ] Create request/response DTOs as Java Records in `dto/`
- [ ] Create a Mapper component in `mapper/`
- [ ] Create a Service interface in `service/`
- [ ] Implement the service in `service/*ServiceImpl.java`
- [ ] Create REST controller in `controller/` with `@RestController` and OpenAPI annotations
- [ ] Create Thymeleaf template if UI is needed
- [ ] Add Kafka message DTO in `messaging/dto/` if async processing is required
- [ ] Add Producer/Consumer in `messaging/` if async processing is required
- [ ] Write unit tests with Mockito (min 50% coverage)
- [ ] Write smoke tests if new endpoints are added
- [ ] Update API docs in `docs/content/api-reference.md`

## Example: BaggageAlert Feature

### 1. Entity
```java
@Entity
public class BaggageAlert {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String userEmail;
    private String origin;
    private String destination;
    private int maxBaggageKg;
    private BigDecimal targetPrice;
    @Enumerated(EnumType.STRING)
    private AlertStatus status;
}
```

### 2. Repository
```java
@Repository
public interface BaggageAlertRepository extends JpaRepository<BaggageAlert, UUID> {
    List<BaggageAlert> findByUserEmail(String userEmail);
}
```

### 3. Service
```java
public interface BaggageAlertService {
    BaggageAlertResponse createAlert(CreateBaggageAlertRequest request);
    List<BaggageAlertResponse> getAlertsByEmail(String email);
}
```

### 4. Controller
```java
@RestController
@RequestMapping("/api/v1/baggage-alerts")
@Tag(name = "Baggage Alerts")
public class BaggageAlertController {
    // POST /, GET /?email=, DELETE /{id}
}
```
