# PetBoby Backend Demo Index

Muc dich:
- Lam muc luc nhanh de tim "kien thuc da hoc" va "dia chi code demo/ung dung" trong du an.
- Moi dong gom: ten kien thuc, vi tri code, endpoint demo (neu co), ghi chu cach doc.

---

## 1) Spring Boot Fundamentals

| Kien thuc | Vi tri code demo/ung dung | Endpoint/ghi chu |
|---|---|---|
| `@SpringBootApplication`, auto-config | `product/src/main/java/com/sonnk/product/ProductApplication.java` | Entry point module `product` |
| `@ConfigurationProperties` (product) | `product/src/main/java/com/sonnk/product/config/ProductAppProperties.java` | Prefix: `petboby.product` |
| Enable properties binding | `product/src/main/java/com/sonnk/product/ProductApplication.java` | `@EnableConfigurationProperties(ProductAppProperties.class)` |
| `@ConfigurationProperties` (order product-client) | `order/src/main/java/com/sonnk/order/config/ProductServiceProperties.java` | Prefix: `petboby.order.product-service` |
| RestTemplate config + timeout | `order/src/main/java/com/sonnk/order/config/RestTemplateConfig.java` | Bean `productServiceRestTemplate` |
| Profile/cau hinh theo moi truong | `product/src/main/resources/application.yml`, `product/src/main/resources/application-dev.yml` | Kiem tra `spring.config.activate.on-profile` |

---

## 2) IoC, DI, Bean Scope, Bean Lifecycle, Circular Dependency

| Kien thuc | Vi tri code demo/ung dung | Endpoint/ghi chu |
|---|---|---|
| Constructor injection | `product/src/main/java/com/sonnk/product/service/ProductService.java` | Service inject repository/properties qua constructor |
| Bean scope (`singleton/prototype/request`) | `product/src/main/java/com/sonnk/product/demo/ScopedBeans.java` | Demo scope |
| Scope demo controller | `product/src/main/java/com/sonnk/product/demo/ProductDemoController.java` | `GET /api/demo/scopes` |
| Bean lifecycle (`@PostConstruct`) | `product/src/main/java/com/sonnk/product/demo/ScopedBeans.java`, `product/src/main/java/com/sonnk/product/utils/CloudinaryUtil.java` | Theo doi log khi bean init |
| JPA lifecycle callback (`@PrePersist/@PreUpdate`) | `product/src/main/java/com/sonnk/product/model/entity/base/BaseEntity.java`, `order/src/main/java/com/sonnk/order/model/entity/base/BaseEntity.java`, `user/src/main/java/com/sonnk/user/model/entity/base/BaseEntity.java` | Audit field auto set |
| Circular dependency note (demo only) | Tim file `CircularDependencyNotes` trong module `product` | JavaDoc ghi ro "demo only, khong vao luong chinh" |

---

## 3) Spring AOP

| Kien thuc | Vi tri code demo/ung dung | Endpoint/ghi chu |
|---|---|---|
| Custom annotation cho logging aspect | `product/src/main/java/com/sonnk/product/demo/DemoLogged.java` | Danh dau method can ap dung aspect |
| `@Aspect` + `@Around` | `product/src/main/java/com/sonnk/product/demo/DemoLoggingAspect.java` | Log execution time |
| Self-invocation limitation | `product/src/main/java/com/sonnk/product/demo/ProductDemoService.java` | Method `selfInvocationEntry()` goi noi bo de minh hoa |

---

## 4) JPA / Hibernate Core

| Kien thuc | Vi tri code demo/ung dung | Endpoint/ghi chu |
|---|---|---|
| Persistence context + dirty checking | `product/src/main/java/com/sonnk/product/service/ProductService.java` | `updateProductBasicInfo()` khong goi `save()` lai |
| State/flush/clear/merge demo | `product/src/main/java/com/sonnk/product/demo/ProductDemoService.java` | `POST /api/demo/jpa`, `POST /api/demo/jpa/{id}` |
| Derived query | `product/src/main/java/com/sonnk/product/repository/ProductRepository.java` | `findByStatus`, `findByStatusAndCategory_Id`, ... |
| SELECT IN (`findByIdIn`) | `product/src/main/java/com/sonnk/product/repository/ProductRepository.java`, `product/src/main/java/com/sonnk/product/service/ProductService.java` | API: `GET /api/products/bulk?ids=1,2,3` |
| N+1 naive vs optimized | `order/src/main/java/com/sonnk/order/demo/OrderNPlusOneDemoService.java`, `order/src/main/java/com/sonnk/order/repository/OrderRepository.java`, `order/src/main/java/com/sonnk/order/demo/OrderNPlusOneDemoController.java` | `GET /api/demo/orders/nplus1/naive`, `GET /api/demo/orders/nplus1/optimized` |
| Soft delete filter entity level | `product/src/main/java/com/sonnk/product/model/entity/Product.java`, `Category.java`, `Topping.java`, `order/src/main/java/com/sonnk/order/model/entity/Order.java` | Dung `@SQLRestriction("deleted_at IS NULL")` |

---

## 5) REST API, Validation, Exception Handling

| Kien thuc | Vi tri code demo/ung dung | Endpoint/ghi chu |
|---|---|---|
| REST + DTO | `product/src/main/java/com/sonnk/product/api/ProductController.java` | CRUD + bulk + toppings endpoint |
| Validation request | `product/src/main/java/com/sonnk/product/api/dto/*` | Kiem tra annotation `jakarta.validation` |
| Global exception handling | `product/src/main/java/com/sonnk/product/api/GlobalExceptionHandler.java`, `order/src/main/java/com/sonnk/order/api/GlobalExceptionHandler.java` | Format loi thong nhat |

---

## 6) Microservices - Sync (HTTP) va Async (Kafka)

| Kien thuc | Vi tri code demo/ung dung | Endpoint/ghi chu |
|---|---|---|
| Port-Adapter cho sync call | `order/src/main/java/com/sonnk/order/application/port/ProductClient.java`, `order/src/main/java/com/sonnk/order/infrastructure/client/ProductRestClient.java` | Order goi Product qua HTTP |
| Use case tao don dung bulk product API | `order/src/main/java/com/sonnk/order/application/service/OrderService.java` | `POST /api/orders` |
| Kafka event model | `order/src/main/java/com/sonnk/order/application/event/OrderCreatedEvent.java`, `product/src/main/java/com/sonnk/product/infrastructure/messaging/OrderCreatedPayload.java` | Co `eventId` ho tro idempotency |
| Kafka producer | `order/src/main/java/com/sonnk/order/infrastructure/messaging/KafkaOrderEventPublisher.java`, `order/src/main/java/com/sonnk/order/config/KafkaProducerConfig.java` | Topic `order.created` |
| Kafka consumer + metadata log | `product/src/main/java/com/sonnk/product/infrastructure/messaging/OrderCreatedKafkaListener.java` | `@KafkaListener` group `product-service` |
| Gateway route | `gateway-service/src/main/resources/application.yml` | Route `/api/orders/**`, `/api/demo/orders/**` |
| So sanh Saga vs Event-Driven (kien thuc kien truc) | `docs/kafka-operations.md` | Muc `6. So sánh Saga và Event-Driven trong PetBoby` |
| Saga choreography skeleton - event contracts | `order/src/main/java/com/sonnk/order/application/event/saga/*.java` | Event + compensation event (`InventoryReleaseRequestedEvent`) |
| Saga choreography skeleton - state machine | `order/src/main/java/com/sonnk/order/model/entity/enums/OrderSagaState.java`, `order/src/main/java/com/sonnk/order/infrastructure/messaging/OrderSagaCoordinatorListener.java` | Cap nhat `sagaState` theo inventory/payment result |
| Saga choreography skeleton - inventory side | `product/src/main/java/com/sonnk/product/infrastructure/messaging/SagaInventoryChoreographyListener.java`, `SagaInventoryEventPublisher.java` | Reserve/release inventory (demo) |
| Saga choreography skeleton - payment side (demo) | `order/src/main/java/com/sonnk/order/infrastructure/messaging/SagaPaymentProcessorListener.java` | Sim payment success/fail theo `branchCode` |
| Idempotent consumer - ProcessedEvent entity (order) | `order/src/main/java/com/sonnk/order/model/entity/ProcessedEvent.java` | Table `processed_events`, unique constraint `(event_id, consumer_group)` |
| Idempotent consumer - repository (order) | `order/src/main/java/com/sonnk/order/repository/ProcessedEventRepository.java` | `existsByEventIdAndConsumerGroup`, `findByProcessedAtBefore` (cleanup) |
| Idempotent consumer - helper (order) | `order/src/main/java/com/sonnk/order/infrastructure/idempotency/IdempotentConsumerHelper.java` | `tryMarkProcessed()` — check + INSERT cùng transaction, chặn race condition bằng unique constraint |
| Idempotent consumer - ProcessedEvent entity (product) | `product/src/main/java/com/sonnk/product/model/entity/ProcessedEvent.java` | Cùng pattern với order |
| Idempotent consumer - helper (product) | `product/src/main/java/com/sonnk/product/infrastructure/idempotency/IdempotentConsumerHelper.java` | Dùng trong `SagaInventoryChoreographyListener` |

---

## 7) Security (Auth/JWT)

| Kien thuc | Vi tri code demo/ung dung | Endpoint/ghi chu |
|---|---|---|
| Auth Security config | `auth/src/main/java/com/sonnk/auth/config/SecurityConfig.java` | Filter chain, auth rules |
| JWT properties | `auth/src/main/java/com/sonnk/auth/config/AuthJwtProperties.java` | Prefix `petboby.auth.jwt` |
| Login API | `auth` module (`/api/auth/login`) | Luong login/issue token |

---

## 8) Cac tai lieu lien quan

- `PetBoby-backend-roadmap.md`
- `PetBoby-backend-knowledge-status.md`
- `PetBoby-backend-knowledge-to-code.md`
- `PetBoby-backend-knowledge-senior.md`
- `PetBoby-backend-coding-rules.md`
- `backend-overview.md`
- `kafka-operations.md`
- `kafka-data-integrity-and-deep-dive.md`

---

## 9) Quy tac cap nhat file nay (bat buoc)

Moi khi them **demo code moi** hoac hoc them **kien thuc moi**, phai cap nhat ngay file nay:
1. Them dong moi vao bang phu hop.
2. Ghi ro:
   - Ten kien thuc.
   - Path file code.
   - Endpoint test/ghi chu cach doc.
3. Neu co doi ten file/class/endpoint, cap nhat lai dong cu ngay trong cung commit.
