## BACKEND KNOWLEDGE vs `PetBoby` ROADMAP

File nay dung de:
- Danh dau nhung noi dung da hoc va da demo trong code `PetBoby`.
- Theo doi phan con thieu theo roadmap backend.
- Lam "bang tien do hoc tap" de tiep tuc hoc len senior.

---

## 1) Tong quan tien do hien tai

- **Phase 1 (Spring core + REST + JPA)**: da hoan thanh phan lon.
- **Phase 2 (Microservices + Sync/Async + Kafka)**: da hoc va da co code demo.
- **Tai lieu bo sung senior**: da co trong `PetBoby-backend-knowledge-to-code.md` va `PetBoby-backend-knowledge-senior.md`.

---

## 2) Phase 1 - Spring Boot Core

### Tuan 1 - Spring Boot fundamentals
- [x] Hieu `@SpringBootApplication`, auto-configuration, component scan.
- [x] Da dung `@ConfigurationProperties` + `@EnableConfigurationProperties`.
- [x] Da dung profile voi `application-dev.yml`.
- [ ] Can tiep tuc review ky starter/dependency cua tung service.

### Tuan 2 - IoC/DI, Bean lifecycle, scope
- [x] Hieu IoC/DI, constructor injection, bean scope.
- [x] Hieu bean lifecycle (`@PostConstruct`, callback, destroy).
- [x] Hieu circular dependency va 3-level cache o muc concept + practical.

### Tuan 3 - RESTful API + exception handling
- [x] Da co DTO request/response ro rang.
- [x] Da co `GlobalExceptionHandler` va map status code hop ly.
- [x] Da ap dung API style nhat quan cho module product/order.

### Tuan 4 - JPA/Hibernate core
- [x] Hieu persistence context, dirty checking, flush/clear/merge.
- [x] Da ap dung derived query (`findBy...`, `findByIdIn`).
- [x] Da demo tranh N+1 (naive vs fetch join).
- [x] Da dung enum string mapping, soft delete pattern.

---

## 3) Phase 2 - Microservices & Communication

### Tuan 1 - Microservices 101
- [x] Hieu Monolith vs Microservices, bounded context.
- [x] Da map kien truc `user/product/order/gateway-service`.
- [x] Tai lieu kien truc: `backend-overview.md`.

### Tuan 2 - API Gateway
- [x] Da route `/api/orders/**` va `/api/demo/orders/**` den order service.
- [x] Da co filter header de demo cross-cutting qua gateway.
- [x] Da tach port gateway/order tranh trung cong.

### Tuan 3 - Sync communication (RestTemplate)
- [x] Da tao `ProductClient` (port) va `ProductRestClient` (adapter).
- [x] Da dung timeout config bang properties.
- [x] Da goi bulk API (`/api/products/bulk`) de giam N network calls.
- [x] Da map loi 404/5xx thanh exception business ro rang.

### Tuan 4 - Async communication (Kafka)
- [x] Order publish `OrderCreatedEvent`.
- [x] Product consume topic `order.created`.
- [x] Da log topic/partition/offset/key o producer va consumer.
- [x] Da bo sung `eventId` de ho tro idempotency.
- [x] Da co tai lieu van hanh: `kafka-operations.md`.
- [x] Da co tai lieu data integrity + deep dive: `kafka-data-integrity-and-deep-dive.md`.

---

## 4) Cac diem da hoc o muc senior (co note)

- [x] Sync vs Async va ly do dung ket hop trong cung luong tao don.
- [x] Trade-off cua event-driven: eventual consistency, idempotency, retry.
- [x] Ranh gioi DDD/Clean Architecture (port-adapter, layer separation).
- [x] Quy tac coding/doc da cap nhat trong `PetBoby-backend-coding-rules.md`.
- [x] Resilience Patterns:
  - [x] Retry: auto-retry with exponential backoff (idempotency check required for POST).
  - [x] Circuit Breaker: 3 states (CLOSED → OPEN → HALF_OPEN), prevent cascade failure.
  - [x] Timeout: TimeLimiter to prevent hanging requests, timeout cascade rule.
  - [x] Rate Limiter: Token Bucket algorithm, local vs global (Redis-backed).
  - [x] Bulkhead: Thread pool isolation, protect slow endpoints from starving fast endpoints.
  - [x] Combined patterns: Stack order matters (TimeLimiter → CircuitBreaker → Retry).

---

## 5) Phan con thieu de tiep tuc hoc

- [x] Security nang cao cho production (JWT hardening, refresh rotation, revoke strategy).
- [ ] Testing nang cao (integration test theo luong nghiep vu, test messaging).
- [ ] Observability (metrics, tracing, dashboard, alerting).
- [x] Resilience patterns (retry policy, circuit breaker, backoff strategy, timeout, rate limiter, bulkhead).
- [x] Saga choreography skeleton (state machine + compensation + idempotent consumer) da duoc implement trong order/product (xem docs/kafka-operations.md + PetBoby-backend-demo-index.md).

---

## 6) Cach su dung file nay moi tuan

1. Chon 1 chu de trong roadmap backend.
2. Hoc ly thuyet + map vao code PetBoby.
3. Danh dau `[x]` khi da:
   - hieu concept,
   - co code demo/ung dung,
   - note duoc cong dung + han che.
4. Neu chi moi hoc ly thuyet ma chua code, giu `[ ]`.

---

## 7) Tai lieu lien quan

- `PetBoby-backend-roadmap.md`
- `PetBoby-backend-knowledge-to-code.md`
- `PetBoby-backend-knowledge-senior.md`
- `PetBoby-backend-coding-rules.md`
- `backend-overview.md`
- `kafka-operations.md`
- `kafka-data-integrity-and-deep-dive.md`

