# ROADMAP: SENIOR BACKEND (JAVA / SPRING / MICROSERVICES) VỚI PROJECT `PetBoby` - 12 THÁNG (ONLY BACKEND VERSION)

> Lưu ý: Roadmap này chỉ cover **Backend Java/Spring + kiến trúc microservices** và dùng trực tiếp project `PetBoby` (`user`, `product`, `order`, `gateway-service`) để thực hành. Frontend/Angular sẽ xây riêng.

## OVERVIEW

- **Thời gian**: ~**1.5h/ngày x 7 ngày ≈ 10.5h/tuần**, tập trung 100% cho backend.
- **Trọng tâm**:
  - Củng cố Spring Boot core, JPA/Hibernate, RESTful API, DI/AOP, security.
  - Hiểu sâu kiến trúc microservices (user/product/order/gateway-service) sẵn có trong `PetBoby`.
  - Làm performance optimization: N+1, caching (Redis), DB index, logging/observability.
  - Làm việc với **Kafka**, **Redis**, **Liquibase**, **Spring Security**, **API Gateway**.
  - Nâng lên mức **system design** + **technical leadership** (code review, estimation, mentoring).
- **Key milestones**:
  - **Phase 1 (Tháng 1–3)**: Hiểu chắc Spring Boot core, JPA, cấu trúc `PetBoby`. Chạy được đủ 3 service + gateway, tự tin thêm CRUD/endpoint mới.
  - **Phase 2 (Tháng 4–6)**: Nắm microservices patterns, event-driven với Kafka, caching, error handling. Có thể build feature full backend trong `PetBoby`.
  - **Phase 3 (Tháng 7–9)**: Thiết kế module/feature mới end-to-end, refactor kiến trúc, thêm observability.
  - **Phase 4 (Tháng 10–12)**: Tập trung security, scalability, resilience, leadership (review, mentoring, docs).
- **Success metrics**:
  - Tự tin giải thích kiến trúc `PetBoby`, flow giữa `user/product/order/gateway-service`.
  - Tự design & implement 1–2 module mới (ví dụ: loyalty, notification) theo chuẩn production.
  - Đọc/hiểu code người khác nhanh, review tốt, estimate feature hợp lý.

---

## PHASE 1: FOUNDATION STRENGTHENING (Tháng 1–3)

**Goal phase**: Lấp gap Java/Spring core (DI, Bean lifecycle, AOP, JPA deep), đọc hiểu toàn bộ code `PetBoby`, tự tin thêm CRUD + logic đơn giản ở từng service.

### Tháng 1: Spring Boot Core & Project `PetBoby` Overview

> Giả định: 10.5h/tuần ~ chia 5 buổi x 2h + 1 buổi nghỉ / tuỳ lịch. Dưới đây chỉ là gợi ý phân bố, không cần quá cứng.

#### Tuần 1: Spring Boot Fundamentals & Run `PetBoby` Services
- **Mục tiêu**: Hiểu lại Spring Boot cơ bản, chạy được từng service `user`, `product`, `order`, `gateway-service`.
- **Backend (~10.5h)**:
  - **Concepts**:
    - Cấu trúc project Spring Boot (pom.xml, `@SpringBootApplication`, `application.yml`).
    - Auto-configuration, `@Configuration`, `@Bean`, profiles.
    - Cách Maven quản lý dependencies (xem `pom.xml` trong `product`, `user`, `order`, `gateway-service`).
  - **Resources**:
    - Spring Boot docs: `https://docs.spring.io/spring-boot/reference/`
    - Spring Guides – Building a RESTful Web Service: `https://spring.io/guides/gs/rest-service/`
    - Baeldung – Intro to Spring Boot: `https://www.baeldung.com/spring-boot`
  - **Practice với `PetBoby`**:
    - Đọc `pom.xml` của 4 module để hiểu dependencies đang dùng: JPA, Redis, Kafka, Security, Liquibase, Gateway.
    - Mở `UserApplication`, `OrderApplication`, `ProductApplication` (nếu có) & Main của `gateway-service`, note:
      - Package root, component scan, các package con: `controller`, `service`, `repository`, `model.entity`, `utils`, ...
    - Thử:
      - Chạy riêng từng service bằng Maven (`mvn spring-boot:run` trong `user`, `product`, `order`, `gateway-service`).
      - Gọi vài endpoint hiện có (nếu có) bằng Postman/Insomnia (ví dụ `/products`, `/users`).
  - **Checklist tự đánh giá**:
    - [ ] Giải thích được ý nghĩa các dependency chính trong `pom.xml` (Spring Web, Data JPA, Redis, Kafka, Security, Liquibase).
    - [ ] Chạy được từng service mà không copy/paste lệnh từ đâu.
    - [ ] Biết các profile / `application.yml` chính đang dùng.
- **Workplace Integration**:
  - Quan sát cấu trúc Spring Boot của dự án công ty, so sánh với `PetBoby` (layering, package).
  - Hỏi senior: “Tại sao team tách thành các module/service như hiện tại? Có rule nào cho package structure không?”.
- **Checkpoint**:
  - Tự viết 5–7 câu giải thích kiến trúc high-level `PetBoby` (các service là gì, nói chuyện với nhau qua đâu).

#### Tuần 2: Dependency Injection, Bean Lifecycle, Configuration
- **Mục tiêu**: Hiểu DI, bean scopes, lifecycle, cấu hình qua `application.yml` và `@ConfigurationProperties`.
- **Backend**:
  - **Concepts**:
    - `@Component`, `@Service`, `@Repository`, `@Controller/@RestController`, `@Configuration`.
    - Constructor injection vs field injection, circular dependency.
    - Bean scope: singleton, prototype, request, session.
    - `@ConfigurationProperties`, `@Value`.
  - **Resources**:
    - Spring Core – IoC Container: `https://docs.spring.io/spring-framework/reference/core/beans.html`
    - Baeldung – Dependency Injection in Spring: `https://www.baeldung.com/inversion-control-and-dependency-injection-in-spring`
    - Baeldung – Spring Bean Scopes: `https://www.baeldung.com/spring-bean-scopes`
  - **Practice với `PetBoby`**:
    - Tìm trong `product`, `user`, `order`:
      - Một vài `@Service`, `@Repository`, `@Controller` – vẽ lại diagram dependency giữa chúng.
    - Tạo 1 class config nhỏ trong `product`:
      - Ví dụ `CloudinaryConfig` hoặc `RedisConfig` (nếu chưa có) sử dụng `@Configuration` + `@ConfigurationProperties`.
    - Refactor 1–2 chỗ nếu đang dùng field injection (`@Autowired` trên field) sang **constructor injection**.
  - **Checklist**:
    - [ ] Giải thích được tại sao nên ưu tiên constructor injection.
    - [ ] Biết chỗ nào trong `PetBoby` tạo ra bean cấu hình (nếu có).
- **Workplace Integration**:
  - Trong code hiện tại ở công ty, tìm 1–2 chỗ có thể refactor sang constructor injection, tạo PR nhỏ.
  - Hỏi senior: “Team mình có guideline về DI/bean không? Có cấm field injection không?”.
- **Checkpoint**:
  - Design 1 service mới nhỏ trong `PetBoby` (ví dụ `EmailNotificationService` dummy log) với constructor injection + interface.

#### Tuần 3: RESTful API Design & Controller-Service-Repository Pattern
- **Mục tiêu**: Thiết kế API chuẩn REST, hiểu rõ flow controller → service → repository.
- **Backend**:
  - **Concepts**:
    - RESTful conventions: resource naming, HTTP methods, status codes.
    - Request/response DTO vs entity.
    - Exception handling cơ bản với `@ControllerAdvice` + `@ExceptionHandler`.
  - **Resources**:
    - Spring Web docs: `https://docs.spring.io/spring-boot/reference/web/index.html`
    - Spring guide – Building RESTful Web Service: `https://spring.io/guides/gs/rest-service/`
    - Baeldung – REST API with Spring: `https://www.baeldung.com/rest-with-spring-series`
  - **Practice với `PetBoby`**:
    - Trong `product`:
      - Đọc entity `Product`, `Category`, `Topping`.
      - Tìm các controller hiện tại (nếu có) – liệt kê các endpoint.
    - Thêm 1 API mới:
      - Ví dụ: `/api/products/{id}/toppings` – trả về danh sách `ToppingInfo` của 1 product, sử dụng logic từ `Product#getToppings()`.
      - Tạo DTO response riêng nếu cần.
    - Thử thêm `@ControllerAdvice` chung cho `product` service để handle NotFound, Validation, InternalError.
  - **Checklist**:
    - [ ] Tạo được 1 controller mới full flow (mapping → service → repository → entity → DTO).
    - [ ] Log error đúng chỗ trong exception handler.
- **Workplace Integration**:
  - Chọn 1 endpoint mới/feature nhỏ ở công ty, tự đề xuất REST contract (path, method, body, response).
  - Hỏi senior: “Team mình có API guideline riêng (status code, error format) không?”.
- **Checkpoint**:
  - Viết 1 tài liệu ngắn (markdown) liệt kê tất cả endpoint của `product` hiện có + endpoint mới bạn vừa thêm.

#### Tuần 4: JPA/Hibernate Essentials & Entity Mapping
- **Mục tiêu**: Nắm chắc mapping cơ bản, quan hệ, lazy/eager, hiểu BaseEntity trong `PetBoby`.
- **Backend**:
  - **Concepts**:
    - Entity lifecycle, primary key generation, `@Id`, `@GeneratedValue`.
    - Quan hệ: `@OneToMany`, `@ManyToOne`, `@ManyToMany`, cascade, orphanRemoval.
    - `FetchType.LAZY` vs `EAGER`.
    - Basic JPQL, derived queries trong Spring Data JPA.
  - **Resources**:
    - Spring Data JPA docs: `https://docs.spring.io/spring-data/jpa/reference/`
    - Vlad Mihalcea blog – JPA & Hibernate best practices: `https://vladmihalcea.com/tutorials/hibernate/`
    - Baeldung – Spring Data JPA: `https://www.baeldung.com/the-persistence-layer-with-spring-data-jpa`
  - **Practice với `PetBoby`**:
    - Đọc `BaseEntity` trong `product`, `order`, `user` – xem có trường `createdAt`, `updatedAt`, soft delete không.
    - Vẽ ERD đơn giản cho:
      - `product` – `category` – `topping`.
      - `user` – `profile` – `employee/customer`.
    - Thêm 1–2 derived query trong `ProductRepository`:
      - Ví dụ: `findByStatusAndCategoryId(...)`, `findByHasToppingTrueAndStatus(...)`.
  - **Checklist**:
    - [ ] Phân biệt được khi nào nên dùng `LAZY` vs `EAGER`.
    - [ ] Hiểu rõ id generation strategy đang dùng trong `PetBoby`.
- **Workplace Integration**:
  - Tìm N+1 trong code công ty (nếu có) bằng cách enable SQL log, nói chuyện với senior về cách fix.
- **Checkpoint**:
  - Viết vài câu trả lời cho 5–7 câu hỏi JPA cơ bản (ví dụ: “LAZY vs EAGER khác gì?”, “CascadeType.ALL là gì?”).

### Side Project 1 (Tháng 1–3): `PetBoby` – Service Audit & Extension
- **Requirements**:
  - Dùng `PetBoby` như monorepo practice:
    - Chạy được cả 3 service `user`, `product`, `order` + `gateway-service`.
    - Thêm ít nhất:
      - 2 endpoint mới cho `product` (ví dụ search theo status/category, API toppings).
      - 1 endpoint mới cho `user` (ví dụ cập nhật profile).
      - 1 endpoint đơn giản cho `order` (ví dụ list order by user).
- **Tech stack**:
  - Java 17, Spring Boot 3.5.x, Spring Web, Spring Data JPA, Redis (nếu cần), Kafka (chưa bắt buộc Phase 1).
- **Timeline**:
  - ~20–25h dàn đều trong 3 tháng (chung với các bài tập week, không thêm hẳn thời gian mới).
- **Learning goals**:
  - Hiểu toàn bộ flow request → gateway → service → DB.
  - Tự thêm tính năng nhỏ không phá vỡ kiến trúc.
- **GitHub repo structure suggestion**:
  - Root: `PetBoby` (giữ nguyên).
  - Thêm thư mục `docs/`:
    - `docs/backend-overview.md` – mô tả kiến trúc + endpoint bạn đã thêm.

---

## PHASE 2: INTERMEDIATE & INTEGRATION (Tháng 4–6)

**Goal phase**: Nắm microservices patterns, Kafka, Redis caching, error handling, logging. Build được full backend feature cross-service trong `PetBoby`.

### Tháng 4: Microservices Basics & Communication Patterns

#### Tuần 1: Microservices 101 & Kiến trúc `PetBoby`
- **Mục tiêu**: Hiểu microservices là gì, pros/cons, `PetBoby` đang tách thành những service nào và vì sao.
- **Backend**:
  - **Concepts**:
    - Monolith vs microservices.
    - Bounded context, service boundaries.
    - Synchronous vs asynchronous communication.
  - **Resources**:
    - Microservices.io – What are microservices: `https://microservices.io`
    - “Microservices Patterns” (Chris Richardson) – website summary: `https://microservices.io/patterns/index.html`
  - **Practice với `PetBoby`**:
    - Đọc code + config, trả lời:
      - `user` chịu trách nhiệm gì? `product`? `order`?
      - `gateway-service` routing như thế nào? (xem `application.yml` của gateway).
    - Vẽ sequence diagram đơn giản: user đặt hàng → request đi từ client → gateway → order → product/user.
  - **Checklist**:
    - [ ] Giải thích được vì sao nên tách `user`/`product`/`order`.
- **Workplace Integration**:
  - Hỏi senior: “Hệ thống hiện tại là monolith hay microservices? Service boundaries được define như thế nào?”.

#### Tuần 2: API Gateway & Routing (Spring Cloud Gateway)
- **Mục tiêu**: Nắm cách `gateway-service` định tuyến request đến các backend service.
- **Backend**:
  - **Concepts**:
    - API Gateway: routing, filtering, cross-cutting concerns.
    - Patterns: API Composition, Backends for Frontends.
  - **Resources**:
    - Spring Cloud Gateway docs: `https://docs.spring.io/spring-cloud-gateway/reference/`
    - Baeldung – Spring Cloud Gateway: `https://www.baeldung.com/spring-cloud-gateway`
  - **Practice với `PetBoby`**:
    - Đọc `application.yml` của `gateway-service`:
      - Liệt kê route mapping `/api/users/**`, `/api/products/**`, `/api/orders/**` (nếu có).
    - Thêm 1 route mới:
      - Ví dụ: `/api/admin/products/**` forward đến `product` với predicate cụ thể.
      - Thử thêm 1 filter đơn giản (log, add header).
  - **Checklist**:
    - [ ] Hiểu rõ request đi từ gateway đến backend như thế nào.
    - [ ] Thêm được route/filter mới mà không phá config cũ.
- **Workplace Integration**:
  - Nếu công ty dùng API Gateway (Kong, Nginx, Spring Cloud Gateway, Apigee), so sánh cách config.

#### Tuần 3: Synchronous Communication – REST, Feign/RestTemplate/WebClient
- **Mục tiêu**: Hiểu cách service gọi nhau qua HTTP, tránh anti-pattern.
- **Backend**:
  - **Concepts**:
    - Inner-service communication qua REST.
    - Timeouts, retries, circuit breaker (overview).
  - **Resources**:
    - RestTemplate (legacy) docs: `https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/client/RestTemplate.html`
    - Spring WebClient: `https://docs.spring.io/spring-framework/reference/web/webflux-webclient.html`
  - **Practice với `PetBoby`**:
    - Nếu đã có chỗ gọi cross-service: tìm, đọc, hiểu.
    - Nếu chưa có: thêm 1 call từ `order` → `product`:
      - Ví dụ trong `order` service, khi tạo order, gọi `product` để get price/product info (mock/simple).
      - Implement bằng `RestTemplate` hoặc `WebClient` với timeout rõ ràng.
  - **Checklist**:
    - [ ] Biết chỗ đặt config timeout cho HTTP client.
    - [ ] Viết được 1 client đơn giản trong `order` để gọi `product`.
- **Workplace Integration**:
  - Tìm trong code công ty chỗ microservice A gọi B, check xem có timeout/retry không.

#### Tuần 4: Asynchronous Basics & Kafka Intro (mới mức cơ bản)
- **Mục tiêu**: Hiểu cơ bản event-driven và Kafka đang dùng trong `PetBoby` để làm gì.
- **Backend**:
  - **Concepts**:
    - Event-driven architecture, eventual consistency.
    - Topic, partition, consumer group.
  - **Resources**:
    - Spring for Apache Kafka docs: `https://docs.spring.io/spring-kafka/reference/`
    - Baeldung – Intro to Spring Kafka: `https://www.baeldung.com/spring-kafka`
  - **Practice với `PetBoby`**:
    - Tìm trong `product`, `order`, `user` chỗ nào dùng Kafka (config, producer, consumer).
    - Nếu đã có event: phân tích 1 flow ví dụ `OrderCreated` → log/notify.
    - Nếu chưa: tạo đơn giản:
      - Producer trong `order` publish event “ORDER_CREATED”.
      - Consumer trong `product` chỉ log nhận được event (hoặc update inventory giả lập).
  - **Checklist**:
    - [ ] Giải thích được flow publish/subscribe trong `PetBoby`.
- **Workplace Integration**:
  - Hỏi senior: “Team mình dùng event-driven chỗ nào? Có pattern gì cho naming event không?”.

### Tháng 5: Persistence Deep-Dive, N+1, Caching (Redis)

#### Tuần 1: Query Optimization & N+1 Problem
- **Mục tiêu**: Nhận diện và fix N+1, tối ưu query cơ bản.
- **Backend**:
  - **Concepts**:
    - N+1 select, join fetch, entity graphs.
    - Pagination vs load all.
  - **Resources**:
    - Vlad Mihalcea – How to detect and fix N+1: `https://vladmihalcea.com/n-plus-1-query-problem/`
    - Baeldung – N+1 problem: `https://www.baeldung.com/hibernate-n1-problem`
  - **Practice với `PetBoby`**:
    - Enable Hibernate SQL logging trong 1 service (ví dụ `product`).
    - Gọi 1 API list product + category/toppings, check log xem có N+1 không.
    - Thử fix:
      - Bằng `@EntityGraph` hoặc `join fetch` query.
  - **Checklist**:
    - [ ] Chỉ rõ được 1 nơi trong `PetBoby` có thể tối ưu query.

#### Tuần 2: Redis Caching với Spring Data Redis
- **Mục tiêu**: Dùng Redis để cache read-heavy endpoints (ví dụ product list).
- **Backend**:
  - **Concepts**:
    - Cache-aside pattern.
    - TTL, cache invalidation basics.
  - **Resources**:
    - Spring Data Redis docs: `https://docs.spring.io/spring-data/redis/reference/`
    - Baeldung – Spring Boot Redis: `https://www.baeldung.com/spring-data-redis-tutorial`
  - **Practice với `PetBoby`**:
    - Trong `product` service:
      - Chọn 1 endpoint thường đọc (ví dụ `/api/products/popular`).
      - Thêm Redis cache sử dụng `@Cacheable`, `@CacheEvict`.
    - Check log/behaviour khi gọi nhiều lần.
  - **Checklist**:
    - [ ] Cài đặt được ít nhất 1 cache layer trong `product`.

#### Tuần 3: Liquibase / DB Migration
- **Mục tiêu**: Hiểu cơ chế migration DB với Liquibase trong `PetBoby`.
- **Backend**:
  - **Concepts**:
    - Changelog, changeset, schema versioning.
  - **Resources**:
    - Liquibase docs: `https://docs.liquibase.com/`
    - Baeldung – Liquibase with Spring Boot: `https://www.baeldung.com/liquibase-refactor-schema-of-java-app`
  - **Practice với `PetBoby`**:
    - Đọc `db.changelog-master.yaml` trong `order`/`product`.
    - Thêm 1 changeset nhỏ:
      - Ví dụ: thêm column `discount` cho `product` hoặc `note` cho `order`.
    - Chạy ứng dụng, confirm migration chạy OK.
  - **Checklist**:
    - [ ] Tự tin thêm/sửa 1 changeset đơn giản.

#### Tuần 4: Logging & Error Handling Strategy
- **Mục tiêu**: Chuẩn hoá logging, error response, trace request xuyên service.
- **Backend**:
  - **Concepts**:
    - Log levels, structured logging (JSON).
    - Correlation ID cho request.
  - **Resources**:
    - Spring Boot logging: `https://docs.spring.io/spring-boot/reference/features/logging.html`
    - Logback + logstash encoder: docs on GitHub `logstash-logback-encoder`
  - **Practice với `PetBoby`**:
    - Check `logback-spring.xml` trong `product`.
    - Chuẩn hoá:
      - 1 format log chung.
      - Thêm filter để inject correlation ID theo header (nếu cần).
    - Đảm bảo các exception handlers return error JSON consistent.
  - **Checklist**:
    - [ ] Biết đọc log `PetBoby` để trace 1 request end-to-end.

### Tháng 6: Integration – Build 1 Feature Full Backend trong `PetBoby`

#### Tuần 1–3: Feature “Order with Toppings & Discount”
- **Mục tiêu**: Thiết kế & implement 1 feature đi qua `product` + `order`.
- **Backend**:
  - **Concepts**:
    - Transaction boundaries.
    - Cross-service validation (product exists, user valid).
  - **Resources**:
    - Spring Transaction Management: `https://docs.spring.io/spring-framework/reference/data-access/transaction.html`
  - **Practice với `PetBoby`**:
    - Requirement gợi ý:
      - Người dùng đặt 1 order gồm nhiều product, mỗi product có toppings (sử dụng `ToppingInfo`).
      - Nếu order total > X, áp dụng discount Y%.
    - Thực hiện:
      - Modify `product` để expose endpoint trả thông tin full product + toppings.
      - Trong `order`, thiết kế DTO `CreateOrderRequest` (list items) + `OrderResponse`.
      - Áp dụng transaction ở `order` service.
      - Tùy chọn: publish Kafka event `ORDER_CREATED`.
  - **Checklist**:
    - [ ] Viết được flow end-to-end, test bằng Postman.

#### Tuần 4: Mini-retro & Hardening
- **Mục tiêu**: Review lại toàn bộ code bạn đã thêm, tối ưu & viết docs.
- **Backend**:
  - Dành tuần này để:
    - Refactor code mới (tách service/utility).
    - Viết docs:
      - `docs/order-flow.md` mô tả luồng “Order with toppings & discount”.
  - **Checkpoint Phase 2**:
    - Self-quiz 10–15 câu về microservices, Kafka, Redis, Liquibase.
    - 1 code challenge: implement 1 endpoint mới trong `user` (ví dụ: search user theo role + paging).

### Side Project 2 (Tháng 4–6): `PetBoby` – Order Feature End-to-End
- **Requirements**:
  - Feature “Order with toppings & discount” như trên, đầy đủ:
    - Endpoint create order, list orders, get order detail.
    - Tích hợp với `product` để validate product & giá.
    - (Optional) Gửi event qua Kafka sau khi tạo order.
- **Tech stack**:
  - Spring Boot, Spring Data JPA, Redis (nếu dùng cache), Kafka (optional).
- **Timeline**:
  - ~30–35h trong 3 tháng (dàn chung vào weekly practice).
- **Learning goals**:
  - End-to-end feature implementation trong bối cảnh microservices.

---

## PHASE 3: ADVANCED & ARCHITECTURE (Tháng 7–9)

**Goal phase**: Bước lên tầm architectural thinking, system design, testing & performance.

### Tháng 7: System Design Basics (Backend Focus) Với `PetBoby`

#### Tuần 1: System Design Fundamentals
- **Mục tiêu**: Nắm vocabulary: scalability, availability, consistency, CAP, load balancing.
- **Backend**:
  - **Resources**:
    - System Design Primer (GitHub): `https://github.com/donnemartin/system-design-primer`
    - ByteByteGo blog (free posts).
  - **Practice với `PetBoby`**:
    - Vẽ high-level system diagram: client → gateway → user/product/order → DB/Redis/Kafka.
    - Note bottlenecks: single DB, single instance service, ...

#### Tuần 2: Data Modeling & Bounded Contexts
- **Mục tiêu**: Biết cách tách/bổ sung bounded context.
- **Backend**:
  - Thiết kế hypothetical module mới: `loyalty` hoặc `notification`.
  - Xác định nó nên thành 1 service riêng hay nằm trong `order`.
  - Vẽ ERD & sequence cho use case chính.

#### Tuần 3: API Design for New Module
- **Mục tiêu**: Thiết kế API cho module mới (không cần implement hết).
- **Backend**:
  - Define REST contract cho `loyalty`:
    - `/api/loyalties`, `/api/loyalties/{userId}`,...
  - Viết 1 doc `docs/loyalty-api-design.md` trong root `PetBoby`.

#### Tuần 4: System Design Monthly Exercise
- **Mục tiêu**: Làm 1 bài system design “cà phê shop ordering” dùng lại `PetBoby`.
- **Backend**:
  - Dựa trên System Design Primer, tự làm bài:
    - Thiết kế hệ thống order cho chuỗi cửa hàng pets/café sử dụng `PetBoby` as base.
  - Viết doc: assumptions, high-level architecture, scaling plan.

### Tháng 8: Testing Strategy (Unit, Integration) trong `PetBoby`

#### Tuần 1: Unit Testing Basics (JUnit, Mockito)
- **Mục tiêu**: Viết được unit test cho service layer.
- **Resources**:
  - Baeldung – JUnit 5 Tutorial: `https://www.baeldung.com/junit-5`
  - Baeldung – Mockito: `https://www.baeldung.com/mockito-series`
- **Practice**:
  - Chọn 1–2 service trong `product`/`order`, viết unit test cho business logic (discount, price calculation).

#### Tuần 2: Integration Testing with Spring Boot Test
- **Mục tiêu**: Hiểu Spring Boot integration test, dùng slice test `@DataJpaTest`, `@WebMvcTest`.
- **Resources**:
  - Spring Boot Testing docs: `https://docs.spring.io/spring-boot/reference/testing/index.html`
- **Practice**:
  - Viết `@DataJpaTest` cho `ProductRepository` (test query).
  - Viết `@SpringBootTest` + `TestRestTemplate` test 1–2 endpoint của `product`.

#### Tuần 3: Contract Testing / API Tests
- **Mục tiêu**: Test contract API không phụ thuộc frontend.
- **Practice**:
  - Dùng RestAssured hoặc chỉ TestRestTemplate để viết API tests.

#### Tuần 4: Testing Review & Coverage
- Phân tích coverage (có thể dùng plugin đơn giản như Jacoco).
- Chọn 1 service nâng coverage lên ~60–70% cho phần bạn chạm vào.

### Tháng 9: Performance & Observability

#### Tuần 1: Profiling & Metrics Basics
- **Mục tiêu**: Biết cách đo, không chỉ cảm tính.
- **Resources**:
  - Spring Boot Actuator docs: `https://docs.spring.io/spring-boot/reference/actuator/index.html`
- **Practice**:
  - Enable Actuator trong 1 service (`product`).
  - Xem metrics cơ bản: HTTP requests, DB connections.

#### Tuần 2: Caching Strategy Review
- Xem lại cache đã làm (Redis).
- Thêm/tối ưu TTL, cache key strategy.

#### Tuần 3: DB Index & Query Plan
- Với `product`/`order` table:
  - Xác định field cần index.
  - Dùng EXPLAIN (MariaDB) cho 1–2 query.

#### Tuần 4: Monthly System Design Exercise
- System design: “High-traffic product catalog service” dựa trên `product`.
- Viết doc giải thích scaling (read replica, cache, CDN – conceptual, dù chưa có frontend).

### Side Project 3 (Tháng 7–9): `PetBoby` – New Module (e.g., Loyalty)
- **Requirements**:
  - Thiết kế & implement minimal `loyalty` module:
    - Có thể là 1 package/module trong `order` hoặc 1 service riêng (tuỳ time).
    - Lưu điểm tích luỹ cho user theo orders.
  - Endpoints CRUD + 1–2 metrics (total points).
- **Timeline**:
  - ~35–40h trong 3 tháng.

---

## PHASE 4: SENIOR-LEVEL SKILLS (Tháng 10–12)

**Goal phase**: Security, scalability, resilience, leadership (code review, mentoring, docs).

### Tháng 10: Security Deep Dive (Spring Security)

#### Tuần 1: Spring Security Basics
- **Resources**:
  - Spring Security docs: `https://docs.spring.io/spring-security/reference/`
  - Baeldung – Spring Security: `https://www.baeldung.com/security-spring`
- **Practice với `PetBoby`**:
  - Review security config hiện tại (nếu có).
  - Xác định endpoints public/private.

#### Tuần 2: JWT-based Authentication (Conceptual / nếu `PetBoby` chưa có)
- Thiết kế flow authentication:
  - Login → issue JWT → use in gateway → forward downstream.
- Implement POC nhỏ (có thể trong `user`).

#### Tuần 3: Authorization & Roles
- Phân quyền: admin, staff, customer.
- Thêm annotation `@PreAuthorize` vào 1–2 endpoint.

#### Tuần 4: Security Review
- Checklist:
  - Password storage, sensitive info in logs, CSRF (nếu có giao diện khác), rate limiting (concept).

### Tháng 11: Resilience & Scalability Patterns

#### Tuần 1: Timeouts, Retries, Circuit Breakers
- **Resources**:
  - Resilience4j docs: `https://resilience4j.readme.io/docs`
  - Patterns on microservices.io.
- **Practice**:
  - Áp dụng Resilience4j cho call `order` → `product` (nếu đã implement).

#### Tuần 2: Idempotency & Message Handling
- Thiết kế idempotency key cho create order.
- Nếu dùng Kafka: đảm bảo không double-process order.

#### Tuần 3: Deployment Considerations (Conceptual)
- Dù bạn không set up DevOps từ zero, học khái niệm:
  - Blue-green, canary, rolling update.
  - Horizontal scaling vs vertical scaling.

#### Tuần 4: Monthly System Design Exercise
- Thiết kế hệ thống chịu tải cao cho flash sale trên `product`/`order`.

### Tháng 12: Leadership – Code Review, Technical Writing, Mentoring

#### Tuần 1: Code Review Practice
- **Resources**:
  - Google Engineering Practices – Code Review: `https://google.github.io/eng-practices/review/`
- **Practice**:
  - Lấy 2–3 PR cũ (ở công ty hoặc trong `PetBoby` giả lập) và viết review comment chất lượng.

#### Tuần 2: Technical Writing & ADR (Architecture Decision Record)
- **Resources**:
  - ADR pattern: `https://adr.github.io/`
- **Practice**:
  - Viết 1–2 ADR cho quyết định kiến trúc trong `PetBoby` (ví dụ: dùng Redis cache cho product).

#### Tuần 3: Mentoring Simulation
- Chuẩn bị tài liệu/bài giảng ngắn 30–45 phút về 1 chủ đề:
  - VD: “Intro to JPA & N+1”, “Microservices 101 với PetBoby”.

#### Tuần 4: Final Review & Portfolio
- Tổng hợp:
  - Docs, ADR, diagram, các feature đã làm trong `PetBoby`.
  - Viết 1 README tổng mô tả kỹ năng backend đã đạt.

### Side Project 4 (Tháng 10–12): `PetBoby` – Production-grade Hardening
- **Requirements**:
  - Hardening các service chính:
    - Security (authz, minimum authN flow).
    - Logging chuẩn, correlation ID.
    - 1–2 resilience pattern (retry/circuit breaker).
  - Viết docs deployment assumption (dù chưa có full DevOps).
- **Timeline**:
  - ~35–40h trong 3 tháng.

---

## KNOWLEDGE CHECKPOINTS (Cuối mỗi Phase)

### Sau Phase 1
- **Quiz (gợi ý)**:
  - 10–15 câu về: DI, bean scope, REST basics, JPA mapping, flow controller–service–repository.
- **Code challenge (2–3h)**:
  - Thêm 1 entity + CRUD đầy đủ trong `product` (VD: `Brand`), map quan hệ với `Product`.
- **Reflection**:
  - 3 câu:
    - Mình hiểu `PetBoby` đến mức nào (1–5)? Chỗ nào đọc code còn chậm?

### Sau Phase 2
- **Quiz**:
  - Microservices vs monolith, gateway, Kafka basics, Redis caching, Liquibase.
- **Code challenge**:
  - Implement 1 flow end-to-end giữa `order` và `product` với transaction + validation.
- **Reflection**:
  - Mình đã tự tin đề xuất thay đổi kiến trúc nhỏ chưa (ví dụ refactor module, thêm cache)?

### Sau Phase 3
- **Quiz**:
  - System design concepts, testing strategy, performance.
- **Code challenge**:
  - Thiết kế + implement 1 feature backend mới từ design doc đến code + test.

### Sau Phase 4
- **Quiz**:
  - Security, resilience, leadership.
- **Code challenge**:
  - Review 1 PR lớn (giả lập) + viết ADR cho quyết định kiến trúc.

---

## SENIOR-LEVEL SKILL DEVELOPMENT (BACKEND-FOCUSED)

### System Design – 1 bài/tháng (Tháng 4–12)
- Mỗi tháng chọn 1 chủ đề liên quan `PetBoby`:
  - High-traffic product catalog, order system, loyalty module, notification service, reporting service, etc.
- Output:
  - 1 file markdown trong `docs/system-design-YYYY-MM.md` (vẽ text + link hình ảnh nếu có).

### Code Review Practice
- Mỗi tuần (bắt đầu từ tháng 6):
  - Review ít nhất 1 PR (ở công ty hoặc tự tạo).
  - Tập trung: correctness, readability, test coverage, performance, security.

### Technical Writing
- Mỗi feature quan trọng trong `PetBoby`:
  - Viết 1 doc nhỏ:
    - Problem, solution, trade-offs, future work.

### Mentoring Preparation
- Chọn 2–3 chủ đề bạn đã rất chắc:
  - JPA & N+1, Spring DI, Microservices basics.
- Chuẩn bị slide/tài liệu, dạy thử cho đồng nghiệp hoặc tự record.

---

## CÔNG CỤ THEO DÕI

### A. Weekly Learning Log Template
- **File gợi ý**: `PetBoby-learning-log.md`
- **Per week**:
  - Tuần X (Tháng Y):
    - **Hours planned / spent**: ...
    - **Topics**: ...
    - **What I implemented in PetBoby**: ...
    - **Problems faced**: ...
    - **Questions to ask senior**: ...

### B. Skills Assessment Matrix (Backend)
- Tạo 1 bảng (1–5) cho các skill:
  - Spring Core, Spring Boot, JPA/Hibernate, SQL/DB Design, Microservices, Kafka, Redis, Security, Testing, System Design, Leadership.
- Mỗi phase update 1 lần.

### C. Project Completion Tracker
- Liệt kê Side Project 1–4:
  - Status: Not started / In progress / Done.
  - Link mã nguồn (branch hoặc repo).

### D. Time Allocation (Backend Only)
- Mỗi tuần ~10.5h:
  - **~6–7h**: Học concept + đọc docs/sách.
  - **~3–4h**: Thực hành trực tiếp trên `PetBoby` (code + test + docs).
- Nếu tuần nào bận:
  - Ưu tiên: **thực hành trên `PetBoby`** trước, docs sau.

---

> Gợi ý sử dụng: Mỗi khi bắt đầu tuần mới, mở file này + tạo entry trong `PetBoby-learning-log.md`. Chọn bài tập cụ thể gắn với `PetBoby` (endpoint nào, service nào) rồi làm luôn trong nhánh riêng, coi như “mini-feature” production để luyện kỹ năng Senior Backend.

