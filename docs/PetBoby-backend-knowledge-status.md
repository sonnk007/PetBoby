## BACKEND KNOWLEDGE vs `PetBoby` ROADMAP

File này tổng hợp những gì bạn đã học (IoC, DI, Bean lifecycle, bean scope, circular dependency & 3-level cache, Spring AOP, JPA core) và so sánh với `PetBoby-backend-roadmap.md` để:

- Đánh dấu **đã hoàn thành** các mục trùng.
- Chỉ ra **phần roadmap đang thiếu** so với depth kiến thức bạn đã đọc.
- Liệt kê **phần roadmap còn lại** bạn sẽ học tiếp.

**Độ sâu Senior:** Xem **`PetBoby-backend-knowledge-senior.md`** cho trade-off, khi nào không nên dùng, lưu ý production, bẫy thường gặp và cách review từng chủ đề.

---

## 1. Những kiến thức bạn đã học và mapping vào roadmap

### 1.1. IoC Container, Bean, Bean Scope, Bean Lifecycle, Circular Dependency

- **Bạn đã học**:
  - Khái niệm IoC, DI, container quản lý bean, so sánh new thủ công vs container.
  - Bean là gì, BeanDefinition, cách định nghĩa bean bằng annotation/config/XML.
  - Các scope: `singleton`, `prototype`, `request`, `session`, `application`, `websocket`, khi dùng/khi tránh.
  - Bean lifecycle chi tiết: load BeanDefinition → instantiate → populate → *Aware callbacks* → `BeanPostProcessor` (before init) → `@PostConstruct`/`afterPropertiesSet` → `BeanPostProcessor` (after init) → destroy callbacks.
  - Circular dependency, điều kiện Spring resolve được hay không; 3-level cache (`singletonObjects`, `earlySingletonObjects`, `singletonFactories`); giới hạn với constructor injection / prototype.

- **Mapping với roadmap**:
  - `PHASE 1 / Tháng 1 / Tuần 2: Dependency Injection, Bean Lifecycle, Configuration`
    - Concepts trong roadmap:
      - `@Component`, `@Service`, `@Repository`, `@Controller/@RestController`, `@Configuration`.
      - Constructor vs field injection, circular dependency.
      - Bean scope: singleton, prototype, request, session.
  - ✅ **Đánh dấu: các concept IoC/DI/Bean/Scope/Bean Lifecycle/Circular dependency trong Tuần 2 bạn đã nắm tốt hơn mức cơ bản.**

### 1.2. Spring AOP

- **Bạn đã học**:
  - Khái niệm AOP, cross-cutting concerns (logging, transaction, security, retry, monitoring).
  - Core concepts: Aspect, Advice, Join point, Pointcut, Target, Proxy.
  - Các loại advice: `@Before`, `@After`, `@AfterReturning`, `@AfterThrowing`, `@Around`.
  - Cơ chế proxy (JDK dynamic proxy vs CGLIB), thời điểm tạo proxy (trong `BeanPostProcessor.afterInitialization`), self-invocation không qua proxy.
  - Giới hạn Spring AOP (public method, chỉ trên bean Spring, không intercept call nội bộ).

- **Mapping với roadmap**:
  - Roadmap có đề cập AOP ở **OVERIEW (L9)** và implicit trong:
    - Phase 2 – Logging, Transactions, Caching: đều dùng AOP/proxy bên dưới.
  - ❗ **Roadmap chưa có 1 tuần riêng cho “Spring AOP fundamentals”** nhưng:
    - Bạn đã học khá sâu → coi như **đã hoàn thành một phần nội dung “DI/AOP” của Phase 1 goal**.

### 1.3. JPA / ORM Core, Persistence Context, Entity Lifecycle, Dirty Checking, Flush, Merge

- **Bạn đã học**:
  - ORM là cầu nối giữa object world và relational world, vấn đề identity, mapping, relationship.
  - JPA là specification, Hibernate là implementation.
  - Persistence Context là trái tim: map `EntityId → ManagedEntity`, snapshot initial, dirty checking.
  - Entity lifecycle & state: **Transient → Managed → Detached → Removed**.
  - `persist()` chỉ đưa vào persistence context, SQL chạy khi flush.
  - Flush vs commit, vai trò của `flush()` và transaction.
  - `clear()` biến tất cả entity thành detached.
  - `merge()` tạo managed copy mới, object trả về khác detached object gốc.
  - Cách tư duy đúng: luôn hỏi *entity đang ở state nào*, *persistence context còn không*, *flush khi nào*.

- **Mapping với roadmap**:
  - `PHASE 1 / Tháng 1 / Tuần 4: JPA/Hibernate Essentials & Entity Mapping`
    - Concepts trong roadmap:
      - Entity lifecycle, `@Id`, `@GeneratedValue`, mapping quan hệ, `LAZY` vs `EAGER`, JPQL, derived queries.
  - ✅ **Đánh dấu: bạn đã đi sâu hơn mức “essentials” cho JPA (persistence context, flush, merge, dirty checking).**

### 1.4. Spring Boot Fundamentals (auto-config, profiles, configuration properties)

- **Bạn đã học & áp dụng vào PetBoby**:
  - Cấu trúc project Spring Boot: hiểu `@SpringBootApplication` kết hợp auto-configuration + component scan cho module `product`.
  - Externalized configuration:
    - Dùng `application.yml` + `application-dev.yml`.
    - Sử dụng `@ConfigurationProperties` (`ProductAppProperties`) + `@EnableConfigurationProperties` để bind `petboby.product.*` theo kiểu type-safe.
  - Profiles:
    - Dùng `application-dev.yml` với `spring.config.activate.on-profile: dev` để tách cấu hình dev (show-sql, logging, default page size) khỏi cấu hình chung.
  - Áp dụng configuration vào business code:
    - `ProductService` dùng `ProductAppProperties` để quyết định `defaultPageSize` khi paginate product list.

- **Mapping với roadmap**:
  - `PHASE 1 / Tháng 1 / Tuần 1: Spring Boot fundamentals (overview)`
    - Concepts: cấu trúc project, auto-config, profiles, đọc `pom.xml`, hiểu starter.
  - ✅ **Đánh dấu: phần “auto-config + profiles + configuration properties” coi như đã nắm & đã có ví dụ thực chiến trong module `product`.**

---

## 2. Đánh giá độ phủ của roadmap so với kiến thức đã học

### 2.1. Phần roadmap đã **được cover và bạn đã (gần như) hoàn thành**

| Roadmap section                                                  | Trạng thái hiện tại của bạn |
|------------------------------------------------------------------|-----------------------------|
| Phase 1 – Tuần 1: Spring Boot fundamentals (overview)            | ✅ Đã học & áp dụng (auto-config, profiles, @ConfigurationProperties) |
| Phase 1 – Tuần 2: DI, Bean Lifecycle, Bean Scope, Circular dep   | ✅ Đã học kỹ (IoC, DI, scope, lifecycle, circular, 3-level cache) |
| Phase 1 – Tuần 3: RESTful API design & exception handling         | ✅ Đã học & áp dụng (ProductController, DTO, GlobalExceptionHandler, GET /api/products/{id}/toppings) |
| Phase 1 – Tuần 4: JPA fundamentals & mapping                     | ✅ Đã học core JPA & PC, flush, merge; đã thêm derived queries (findByStatus, findByStatusAndCategory_Id) |
| OVERVIEW: “DI/AOP”                                               | ✅ AOP fundamentals đã nắm  |

> Gợi ý thực tế: với Tuần 2 & Tuần 4 Phase 1, bạn có thể **chuyển trạng thái trong log của mình thành “Review + Practice trên PetBoby” thay vì “Học mới từ lý thuyết”**.

### 2.2. Phần kiến thức bạn đã học nhưng roadmap **chưa ghi rõ / còn thiếu depth**

- **A. IoC internal flow & 3-level cache for circular dependency**
  - Roadmap chỉ ghi “constructor vs field injection, circular dependency” (level concept).
  - Bạn đã biết:
    - `ApplicationContext.refresh()` → load BeanDefinition → `createBean()` → 3-level cache.
    - Tại sao constructor injection + prototype không resolve circular dependency.
  - 👉 Không nhất thiết phải sửa file roadmap, nhưng:
    - Trong **Weekly Learning Log**, bạn nên note rằng **“circular dependency + 3-level cache đã hiểu ở mức implementation”**, tuần học chỉ cần ôn lại bằng cách đọc code Spring (nếu muốn) và áp dụng tránh design smell trong `PetBoby`.

- **B. JPA persistence context & flush/merge/clear ở mức chi tiết**
  - Roadmap tuần 4 nói “Entity lifecycle, mapping, JPQL” nhưng không tách riêng phần:
    - State machine (Transient/Managed/Detached/Removed).
    - Dirty checking & snapshot.
    - `flush()`, `clear()`, `merge()`.
  - 👉 Trong thực hành Phase 1 – Tuần 4, bạn có thể:
    - Tự thêm mục “Review PC, flush, merge” trong log và **coi phần JPA cơ bản ở roadmap là đã covered, chỉ thiếu bài tập áp dụng trên `PetBoby`**.

- **C. Spring AOP chi tiết (proxy, self-invocation, hạn chế)**
  - Roadmap chỉ nhắc AOP như “nội công” cho transaction/logging/caching, không có 1 tuần stand-alone.
  - Bạn đã hiểu:
    - Proxy tạo lúc nào, self-invocation limitations.
    - Giới hạn Spring AOP vs AspectJ.
  - 👉 Khi đến Phase 2 (Logging, Transactions, Caching) bạn chỉ cần:
    - Ghi chú trong log: **“Không cần học lại AOP fundamentals – focus vào pattern áp dụng trên `PetBoby` (transaction boundary, logging cross-service)”**.

---

## 3. Những phần trong roadmap bạn **chưa học** (hoặc mới lướt) – ưu tiên sắp tới

Đây là danh sách high-level (backend-only) bạn nên coi là **TODO lớn** trong 12 tháng:

- **Spring Boot Core level project** (Phase 1 – Tuần 1):
  - Cấu trúc project, auto-config, profiles, đọc kỹ `pom.xml` của các module `PetBoby`.
- **RESTful API design chi tiết & exception handling** (Phase 1 – Tuần 3).
- **Microservices & communication patterns** (Phase 2 – Tháng 4):
  - Monolith vs microservices, bounded contexts.
  - Spring Cloud Gateway, REST client (RestTemplate/WebClient), Kafka.
- **Persistence deep-dive trên thực tế** (Phase 2 – Tháng 5):
  - N+1, join fetch, entity graphs.
  - Redis cache (`@Cacheable`, `@CacheEvict`).
  - Liquibase migration.
- **Feature end-to-end “Order with toppings & discount”** (Phase 2 – Tháng 6).
- **System design, testing, performance, observability** (Phase 3).
- **Security, resilience, leadership** (Phase 4).

---

## 4. Checklist chi tiết – từng tuần Phase 1 với trạng thái hiện tại

### Phase 1 – Tháng 1

- **Tuần 1 – Spring Boot fundamentals & run PetBoby**
  - [x] Hiểu vai trò `@SpringBootApplication`, auto-config & component scan.
  - [x] Áp dụng `@ConfigurationProperties` + `EnableConfigurationProperties` (`ProductAppProperties`).
  - [x] Tạo & sử dụng `application-dev.yml` với profile `dev` (show-sql, logging, default-page-size).
  - [ ] Đọc kỹ `pom.xml` của tất cả service, hiểu dependencies từng starter.
  - [ ] Chạy tất cả service `user/product/order/gateway-service` + gọi 1–2 API để quen tổng flow.
  - **Status gợi ý**: *Lý thuyết & ví dụ trên `product`: DONE – Còn lại: đọc full pom + chạy full hệ thống*.

- **Tuần 2 – DI, Bean Lifecycle, Configuration**
  - [x] Hiểu IoC, DI, bean, bean scope.
  - [x] Hiểu bean lifecycle & circular dependency + 3-level cache.
  - [ ] Áp dụng vào `PetBoby`: refactor 1–2 service sang constructor injection, thêm 1 `@Configuration` class.
  - **Status gợi ý**: *Lý thuyết: DONE – Thực hành trên `PetBoby`: TODO*.

- **Tuần 3 – RESTful API design**
  - [x] REST conventions, DTO vs entity, exception handling.
  - [x] ProductController đầy đủ (GET/POST/PUT/DELETE, DTO, @Valid), GlobalExceptionHandler, GET `/api/products/{id}/toppings`.
  - **Status**: *DONE – Đã áp dụng trong module `product`.*

- **Tuần 4 – JPA/Hibernate essentials**
  - [x] Hiểu ORM, JPA vs Hibernate, persistence context, flush, merge, clear, dirty checking.
  - [x] Derived queries trong `ProductRepository`: `findByStatus`, `findByStatusAndCategory_Id`, `findByHasToppingTrueAndStatus`; API `GET /api/products?status=ACTIVE&categoryId=...`.
  - **Status**: *DONE – Lý thuyết + thực hành derived query trên `PetBoby`.*

---

### Phase 1 – Tháng 1: Tổng kết

- **Tiến độ Tháng 1**: Các tuần 1–4 đã hoàn thành (Spring Boot fundamentals, DI/Bean, RESTful API + exception handling, JPA + derived queries + N+1 + findByIdIn).
- **Bước tiếp theo (Phase 2 – Tháng 4)**: Microservices 101 ✅ bắt đầu; API Gateway, service-to-service (RestTemplate/WebClient), Kafka intro.

---

## 6. Phase 2 – Tháng 4: Microservices Basics & Communication

- **Tuần 1 – Microservices 101 & Kiến trúc PetBoby**
  - [x] Khái niệm: Monolith vs microservices, Bounded context, Sync vs Async.
  - [x] PetBoby: user / product / order / gateway-service — trách nhiệm từng service; gateway routing (`application.yml`: `/api/users/**` → 8081, `/api/products/**` → 8082).
  - [x] Tài liệu: `PetBoby/docs/backend-overview.md` (kiến trúc, route, endpoint); `PetBoby-backend-knowledge-to-code.md` mục 7.
  - [ ] Tự làm: vẽ sequence diagram “user đặt hàng” (client → gateway → order → product); giải thích vì sao tách user/product/order.
  - **Status**: *Lý thuyết & mapping PetBoby: DONE – Practice: vẽ diagram + nói rõ lý do tách service.*

- **Tuần 2 – API Gateway & Routing**
  - [x] Đọc `application.yml` gateway: route theo Path predicate, uri backend.
  - [x] Thêm route `/api/orders/**` → order-service (8083), `/api/demo/orders/**` → order (8083).
  - [x] Thêm filter `AddRequestHeader=X-Gateway-Route, order-service` (và order-demo) để minh hoạ cross-cutting concern.
  - [x] Order service cấu hình `server.port: 8083` để không trùng gateway (8080).
  - **Status**: *DONE – Route + filter đã áp dụng; xem `docs/backend-overview.md` và gateway `application.yml`.*

- **Sync vs Async (bổ sung – đọc cùng Tuần 3 & 4)**  
  - Trong **`PetBoby-backend-knowledge-to-code.md`** mục **6.2.0** có phần dạy rõ: Sync vs Async là gì, khi nào dùng sync (cần kết quả ngay), khi nào dùng async (chỉ thông báo), tại sao lộ trình dạy sync trước async, và **tại sao trong PetBoby cùng luồng “tạo đơn” dùng cả hai** (sync lấy giá, async publish event). Nên đọc 6.2.0 trước 6.2 và mục 7 để nắm bức tranh chung.
- **Tuần 3 – RestTemplate/WebClient**
  - [x] Cấu hình `ProductServiceProperties` (base-url, connect-timeout-ms, read-timeout-ms) + `RestTemplateConfig` bean với timeout.
  - [x] Port `ProductClient` (application layer) + implementation `ProductRestClient` (infrastructure) dùng RestTemplate; gọi GET /api/products/{id} và GET /api/products/bulk?ids=...
  - [x] OrderService.createOrder: gọi `productClient.getProductsByIds(ids)` một lần thay vì N lần getById; validate productId, snapshot giá/tên vào OrderItem.
  - [x] OrderController: POST /api/orders (CreateOrderRequest), GET /api/orders, GET /api/orders/{id}.
  - [x] Exception: ProductNotFoundException → 400; ProductServiceUnavailableException → 503.
  - **Status**: *DONE – Synchronous communication Order → Product đã áp dụng theo DDD (port/adapter), timeout và bulk API.*
- **Tuần 4 – Kafka intro**
  - [x] Order: Kafka config (bootstrap-servers, producer JsonSerializer), `OrderCreatedEvent`, port `OrderEventPublisher`, infra `KafkaOrderEventPublisher` (KafkaTemplate), publish sau khi `createOrder` lưu đơn.
  - [x] Product: Kafka config (consumer group product-service, JsonDeserializer), `OrderCreatedPayload` (cùng shape JSON), `@KafkaListener` topic "order.created" chỉ log.
  - [x] **Quản lý topic, consumer, offset, log message**: Mục 7.5 trong knowledge-to-code; `docs/kafka-operations.md` (lệnh topic/group/offset). Code: producer log topic/key/payload + partition/offset; consumer dùng `ConsumerRecord` log topic/partition/offset/key + payload; bean `NewTopic` trong Order tạo topic `order.created` (2 partition).
  - [x] **Tính toàn vẹn dữ liệu (giao dịch/thanh toán), cơ chế sâu, ứng dụng thực tế**: Mục 7.6; `docs/kafka-data-integrity-and-deep-dive.md` – idempotent consumer, transactional outbox, saga, exactly-once Kafka, partition key; cơ chế log/replication/acks/producer/consumer/transaction; áp dụng PetBoby (eventId trong OrderCreatedEvent cho idempotency).
  - **Status**: *DONE – Event-driven + quản lý topic/offset + tài liệu toàn vẹn dữ liệu & cơ chế sâu Kafka. OrderCreatedEvent có eventId (UUID) sẵn cho idempotent consumer.*

---

## 5. Đề xuất sử dụng 2 file `.md` song song

- **`PetBoby-backend-roadmap.md`**:
  - Giữ vai trò **lộ trình gốc** (không sửa nhiều, tránh rối).
- **`PetBoby-backend-knowledge-status.md` (file này)**:
  - Dùng để:
    - Đánh dấu `[x]/[ ]` từng tuần/mục.
    - Ghi chú: “đã học lý thuyết rồi, tuần này chỉ làm practice trên `PetBoby`”.
    - Bổ sung depth (IoC 3-level cache, PC/flush/merge, AOP) mà roadmap chỉ ghi high-level.

Khi bắt đầu mỗi tuần, bạn:

- Mở **roadmap** để biết chủ đề & bài tập trên `PetBoby`.
- Mở **knowledge-status** để:
  - Tích thêm `[x]` những gì đã xong.
  - Ghi chú chỗ nào đã biết lý thuyết để tập trung vào coding trong `PetBoby`.

