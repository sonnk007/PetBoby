## BACKEND CODING RULES – `PetBoby`

Mục tiêu:  
Viết code theo tư duy **Domain-Driven Design (DDD) + Clean Architecture**, tối ưu cho việc học lên Senior, và giúp AI gen code về sau tuân theo cùng style.

---

## 1. Phân tầng (layering) & package structure

- **Tư duy tổng thể** (per service: `product`, `order`, `user`):
  - `api` (hoặc `web`): controller, DTO request/response, mapping sang application layer.
  - `application` (hoặc `service`): use case / application service, điều phối nghiệp vụ, transaction boundary.
  - `domain`: model nghiệp vụ + logic cốt lõi (entity, value object, domain service, enum).
  - `infrastructure`: repository JPA, client bên ngoài (Kafka, Redis, HTTP), config kỹ thuật.
- **Quy tắc phụ thuộc**:
  - `api` phụ thuộc `application` + DTO.
  - `application` phụ thuộc `domain` + interface repository.
  - `domain` **KHÔNG phụ thuộc** vào `api` / `infrastructure`.
  - `infrastructure` implement interface repository / client được định nghĩa ở `domain` hoặc `application`.

_Hiện tại project đang ở mức "DDD light" (domain + service + api). Khi phát triển tiếp sẽ dần tách rõ `application`/`infrastructure` hơn._

---

## 2. Domain-Driven Design – cách model hoá nghiệp vụ

- **Domain model trước, database sau**:
  - Khi thêm tính năng mới:
    - Bước 1: Viết mô tả domain trong `PetBoby-domain-knowledge.md`.
    - Bước 2: Thiết kế entity/value object/enums trong package `model.entity`/`domain`.
    - Bước 3: Mới đến mapping JPA & migration DB.
- **Entity / Value Object / Aggregate** (mức đơn giản):
  - Entity:
    - Có identity (`id`) ổn định, sống lâu (vd: `Product`, `Category`, `Order`, `UserModel`).
  - Value Object:
    - Không id riêng, bất biến, equality theo value (vd sau này: `Money`, `Address`, `Percentage`).
  - Aggregate:
    - Nhóm entity/value object mà bạn thường thao tác cùng nhau (vd: `Order` + `OrderItem` + `OrderItemTopping`).
- **Rule**:
  - Logic nghiệp vụ phức tạp nên nằm trong:
    - Method của domain entity (vd: `order.calculateTotals()`).
    - Hoặc domain service (vd: `LoyaltyCalculator`).
  - Tránh nhét toàn bộ logic vào controller.

---

## 3. Clean API: Controller + DTO + Validation + Error handling

- **Controller (`api` layer)**:
  - Chỉ làm 3 việc:
    1. Nhận request DTO (`@RequestBody`, `@PathVariable`, `@RequestParam`), validate với `@Valid`.
    2. Gọi application/service (vd: `ProductService`) thực thi use case.
    3. Map kết quả ra response DTO + trả HTTP status hợp lý.
  - Không:
    - Chứa logic nghiệp vụ phức tạp.
    - Tự đụng vào repository.
- **DTOs**:
  - Request DTO:
    - Dùng `jakarta.validation` annotation để validate input (không validate trong controller/service nếu không cần).
    - Ví dụ: `ProductCreateRequest`, `ProductUpdateRequest`.
  - Response DTO:
    - Shape JSON trả cho client (vd: `ProductResponse`, `ApiError`).
    - Không expose toàn bộ entity fields nếu không cần.
- **Error handling**:
  - Mỗi service cần 1 `GlobalExceptionHandler` (`@RestControllerAdvice`) với:
    - Handler cho not found (404).
    - Handler cho validation (400).
    - Handler generic (500).
  - Format JSON error thống nhất (`ApiError`).

---

## 4. Service layer (Application / Service)

- **`*Service` (như `ProductService`)**:
  - Đặt trong package `service` hoặc `application`.
  - Nhiệm vụ:
    - Xác định transaction boundary (`@Transactional`).
    - Điều phối repository/domain entity để thực thi **1 use case** cụ thể.
  - Không:
    - Làm việc trực tiếp với HTTP (`ResponseEntity`, annotation web).
    - Biết gì về UI.
- **Rule DI**:
  - Luôn dùng **constructor injection**.
  - Không dùng field injection (`@Autowired` trên field).

---

## 5. Repository & Infrastructure

- Repository:
  - Interface JPA (`extends JpaRepository<...>`) đặt trong `repository` (hạ tầng).
  - Dùng ở service/application layer.
- **Giải thích công dụng câu/method JPA (bắt buộc khi gen code)**:
  - Mỗi khi thêm hoặc dùng method repository (derived query hoặc `@Query`), phải có **JavaDoc hoặc comment ngắn** giải thích:
    - **Phần đầu (find/get/query/delete/count/exists)**:
      - `find…` → trả về entity/list (SELECT).
      - `get…` → thường dùng khi chắc chắn có 1 kết quả (nếu không có có thể throw).
      - `exists…` → trả về boolean (SELECT EXISTS).
      - `count…` → trả về số lượng (COUNT).
      - `delete…` / `remove…` → xóa theo điều kiện (DELETE).
    - **Phần By và sau By (điều kiện)**:
      - `By` → bắt đầu danh sách điều kiện WHERE (theo property của entity).
      - `And` / `Or` → nối nhiều điều kiện.
      - `Id` → điều kiện theo primary key (vd: `findById(Long id)`).
      - `IdIn(Collection<Long>)` → WHERE id IN (…) — lấy nhiều bản ghi theo danh sách id.
      - `Status` / `Category_Id` → điều kiện theo property (vd: `findByStatusAndCategory_Id`).
      - `OrderBy…Asc` / `OrderBy…Desc` → sắp xếp.
      - `First` / `Top` → giới hạn số dòng (vd: `findFirst10By…`).
  - Ví dụ cần ghi chú trong code:
    - `findById(Long id)` → tìm một entity theo PK; trả về `Optional`; thường dùng kèm `orElseThrow` ở service.
    - `findByIdIn(Collection<Long> ids)` → tìm tất cả entity có id nằm trong tập ids; tránh gọi `findById` trong vòng lặp (N+1).
    - `findByStatusAndCategory_Id(…)` → lọc theo status và khóa ngoại category.
    - `findWithItemsAndToppingsBy…` (custom `@Query` join fetch) → load kèm collection để tránh N+1.
  - Mục tiêu: người đọc (và bạn học) hiểu **method đó dùng để làm gì**, **khi nào dùng**, **khác gì method tương tự** (vd: `findById` vs `findByIdIn`).
- Kết nối ngoài (Kafka, Redis, HTTP client,...):
  - Tạo client/class trong package `infra`/`client`/`utils`.
  - Nếu phức tạp, wrap thành interface ở domain/application, implementation trong infrastructure.

---

## 6. Spring Boot Fundamentals – quy tắc chung

- **Cấu hình**:
  - Config chia theo:
    - `application.yml` (chung).
    - `application-<profile>.yml` (dev, prod, ...).
  - Mọi group config nên có:
    - 1 class `@ConfigurationProperties` (vd: `ProductAppProperties`).
    - Enable bằng `@EnableConfigurationProperties`.
- **Profiles**:
  - Dùng `spring.config.activate.on-profile` trong file `application-<profile>.yml`.
  - Dev profile:
    - `show-sql`, log DEBUG cho package hiện tại.
  - Prod profile (sau này):
    - Log ít hơn, không show SQL, cấu hình DB/cache/security thật.

---

## 7. Deprecation & cấu hình hợp lệ

- **Thư viện / API bị deprecate**:
  - Khi phát hiện (hoặc khi gen code) dùng API/class/annotation/thư viện đã bị đánh dấu deprecate: **tự động cập nhật** sang method hoặc thư viện thay thế được khuyến nghị.
  - Ví dụ: `authorizeRequests()` → `authorizeHttpRequests()` (Spring Security 6); `javax.*` → `jakarta.*` (Spring Boot 3); bỏ cấu hình Hibernate dialect khi framework tự detect được.
- **Cấu hình YAML (application.yml)**:
  - Chỉ dùng property key còn hợp lệ theo phiên bản Spring Boot đang dùng; không dùng property đã deprecated hoặc đổi tên.
  - Cấu trúc YAML đúng thụt dòng (indentation): key con nằm dưới key cha, không lẫn cấp (vd: `spring.jpa` phải là con của `spring`, không nằm dưới `spring.datasource`).
- **Quét dự án**: Sau khi thêm/sửa code hoặc config, quét lại toàn bộ module để thay thế deprecated API và sửa cấu hình không hợp lệ.

---

## 8. Quy tắc về dependency & imports khi gen code

- **Khi dùng annotation / class thuộc thư viện nào đó, phải đảm bảo**:
  - Kiểm tra `pom.xml` module hiện tại đã có dependency tương ứng chưa:
    - Bean Validation → `spring-boot-starter-validation`.
    - JPA → `spring-boot-starter-data-jpa`.
    - Web → `spring-boot-starter-web`.
    - AOP → `spring-boot-starter-aop`.
    - v.v.
  - Nếu chưa có → thêm dependency **trước** khi dùng.
- **Imports**:
  - Ưu tiên namespace `jakarta.*` (Spring Boot 3+) cho Servlet, JPA, Validation, v.v.
  - Tránh import class cũ `javax.*` cho các API đã chuyển sang Jakarta (vd: javax.servlet → jakarta.servlet).
  - Lưu ý: `javax.crypto` (Mac, SecretKeySpec...) thuộc Java SE, không deprecated, giữ nguyên.

---

## 9. Khi gen code mới (rule cho AI & cho chính bạn)

- Trước khi tạo feature:
  1. Cập nhật domain trong `PetBoby-domain-knowledge.md` (mô tả nghiệp vụ).
  2. Xem lại `PetBoby-backend-knowledge-to-code.md` để reuse pattern cũ (IoC, AOP, JPA, REST, exception).
- Khi gen code:
  - Luôn:
    - Chọn tầng phù hợp (api/service/domain/infra).
    - Tạo DTO thay vì dùng entity trực tiếp ở controller.
    - Đặt `@Transactional` ở service layer, không ở controller.
    - Bổ sung dependency tương ứng trong `pom.xml` nếu dùng annotation/class mới.
    - **Với repository JPA**: giải thích công dụng method (find/get/By/And/In/OrderBy/First…) bằng JavaDoc — xem mục 5.
  - Nếu cần demo kiến thức (không phải nghiệp vụ thật):
    - Đặt vào package `demo` riêng.
    - Ghi rõ JavaDoc: "demo only, không dùng trong luồng chính".
    - Tránh để annotation (`@Component`, `@Service`) nếu demo có thể phá app (vd circular dependency).

- **Giải thích design choice sau khi gen code (bắt buộc)**:
  - Mỗi khi áp dụng một pattern/mindset kiến trúc (DDD, Clean Architecture, Event-Driven, CQRS, v.v.):
    - Thêm comment JavaDoc ngắn (hoặc cập nhật file markdown kiến thức) giải thích:
      - Pattern gì đang được áp dụng.
      - **Công dụng / lợi ích** trong bối cảnh `PetBoby` (tại sao tốt).
      - **Hạn chế / trade-off** (khi nào không nên lạm dụng).
  - Mục tiêu:
    - Không chỉ "dùng pattern cho hay", mà **hiểu sâu** và nhớ được lý do.

- **Cập nhật mục lục demo-knowledge (bắt buộc mỗi lần thêm demo/kiến thức)**:
  - File mục lục chuẩn: `PetBoby-backend-demo-index.md` (trong `docs`).
  - Mỗi lần thêm mới hoặc chỉnh sửa demo code/kiến thức đã học, phải cập nhật file này **trong cùng lần thay đổi**:
    1. Tên kiến thức vừa thực hành/học.
    2. Địa chỉ code cụ thể (path class/file).
    3. Endpoint demo hoặc ghi chú cách chạy/đọc nhanh.
  - Nếu refactor đổi tên class/path/endpoint thì phải sửa lại mục tương ứng trong `PetBoby-backend-demo-index.md` ngay, không để link chết.
  - Mục tiêu: luôn có một "bản đồ học tập + code demo" để tìm nhanh đúng vị trí code.

---

## 10. Tiêu chuẩn Enterprise – 5 triệu concurrent users

> Rule này đặt **ngưỡng chất lượng tối thiểu** cho mọi đoạn code được thêm vào `PetBoby`.  
> Mục tiêu: code phải chịu được tải lớn, dễ maintain, dễ nâng cấp như một hệ thống enterprise thực tế.

---

### 10.1. Checklist bắt buộc TRƯỚC khi viết code mới

Trước khi bắt tay vào code một feature/fix, phải hoàn thành các bước sau:

1. **Review kiến trúc hiện tại** (`docs/backend-overview.md`):
   - Service liên quan là gì? Package nào chứa logic gần nhất?
   - Có service/topic/repo nào đang làm gần giống không? Reuse thay vì tạo mới.

2. **Kiểm tra impact**:
   - Thay đổi này có ảnh hưởng transaction boundary không?
   - Có tạo N+1 mới hoặc block thread không cần thiết không?
   - Có introduce distributed transaction mà chưa có compensation không?

3. **Xác định load profile**:
   - Endpoint này sẽ được gọi bao nhiêu lần/giây? Có cần cache không?
   - Dữ liệu có thể đọc từ read-replica/cache thay vì primary DB không?
   - Có cần queue/async thay vì sync call trực tiếp không?

4. **Review code cũ liên quan**: đọc file/class sẽ bị ảnh hưởng trước khi viết.

---

### 10.2. Tiêu chuẩn code – các trụ cột chính

#### A) Concurrency & Thread Safety
- Không dùng shared mutable state trong singleton bean (Spring bean mặc định là singleton).
- Dùng `AtomicLong`, `ConcurrentHashMap`, hoặc `ThreadLocal` khi cần state per-request.
- Tránh `synchronized` block trên hot path; ưu tiên lock-free hoặc tách dữ liệu theo shard/partition.

#### B) Database – tránh bottleneck
- **Không query N+1**: mọi query load collection phải dùng `join fetch`, `@EntityGraph`, hoặc `findByIdIn`.
- **Index trước khi query**: mọi column làm WHERE/ORDER BY/JOIN phải được index. Đánh note index trong entity comment.
- **Pagination bắt buộc**: mọi list API phải dùng `Pageable`, không trả toàn bộ bảng.
- **Read/Write tách biệt**: `@Transactional(readOnly = true)` cho mọi query không ghi; sẵn sàng route sang read-replica.
- **Connection pool**: cấu hình HikariCP đúng pool size theo công thức `(cpu_cores * 2) + disk_spindles`; không để default khi lên prod.

#### C) Caching
- Mọi dữ liệu ít thay đổi và đọc nhiều (category, product menu) phải có cache layer (`@Cacheable` + Redis).
- Cache key phải **tường minh và có namespace** (tránh collision giữa service).
- Phải có `@CacheEvict` rõ ràng khi write để tránh stale data.
- TTL cache phải được define trong config (không để mặc định vô hạn).

#### D) Async & Non-blocking
- I/O bên ngoài (Kafka, HTTP call, file) phải async hoặc cấu hình timeout cứng; không để thread block vô hạn.
- Mọi HTTP client (RestTemplate/WebClient) phải có `connectTimeout` + `readTimeout`.
- Kafka consumer phải có idempotency check trước khi xử lý nghiệp vụ quan trọng.

#### E) API Design cho scale
- Endpoint idempotent cho POST (tạo đơn, thanh toán): client gửi `idempotency-key` header, server check duplicate trước khi xử lý.
- Tách `command` (write) và `query` (read) rõ ràng (CQRS light): service write không trả toàn bộ entity, chỉ trả id và status.
- Sử dụng cursor-based pagination cho danh sách lớn thay vì offset (offset chậm dần khi data lớn).

#### F) Resilience & Fault Tolerance
- External call (HTTP, Kafka produce) phải có retry policy với exponential backoff.
- Kafka consumer lỗi sau N retry phải vào DLQ (Dead Letter Queue), không drop message.
- Circuit breaker cho HTTP client giữa service (Resilience4j) khi lên production.

#### G) Observability
- Mọi service operation phải log **correlation-id** / **trace-id** để trace request xuyên suốt microservice.
- Structured log (JSON format cho production) để dễ ship vào ELK/Datadog.
- Metric quan trọng cần expose: request count, latency p99, error rate, Kafka consumer lag.

#### H) Security
- Không log sensitive data (password, token, số thẻ, PII) ở bất kỳ level nào.
- Mọi input từ client phải được validate (`@Valid` + custom validator nếu cần).
- JWT secret/API key không được hardcode, phải đến từ env variable hoặc secret manager.

---

### 10.3. Auto-review & refactor checklist (chạy trước mỗi PR/commit quan trọng)

Khi review/refactor code cũ, kiểm tra theo thứ tự:

| Hạng mục | Câu hỏi kiểm tra | Hành động nếu vi phạm |
|---|---|---|
| **N+1 query** | Có loop gọi `findById` không? | Đổi sang `findByIdIn` + batch |
| **Pagination** | API list có `Pageable` chưa? | Thêm `Pageable` + trả `Page<T>` |
| **Timeout** | HTTP/Kafka có timeout không? | Thêm vào config/client |
| **Idempotency** | Consumer có check duplicate không? | Thêm `processed_events` check |
| **Cache** | Dữ liệu đọc nhiều có cache không? | Thêm `@Cacheable` + Redis |
| **Thread safety** | Bean có mutable field không? | Chuyển sang stateless hoặc dùng `ThreadLocal` |
| **Transaction scope** | `@Transactional` có quá rộng không? | Tách use case nhỏ hơn |
| **Log sensitive** | Log có in token/password không? | Xoá hoặc mask |
| **Index DB** | WHERE column có index chưa? | Thêm index + migration script |
| **DLQ** | Kafka consumer lỗi đi đâu? | Thêm DLQ config |

---

### 10.4. Thứ tự ưu tiên khi phải chọn trade-off

Khi hai nguyên tắc xung đột nhau, ưu tiên theo thứ tự:

1. **Correctness** – đúng nghiệp vụ, không mất dữ liệu.
2. **Reliability** – hệ thống chạy ổn, có retry/fallback.
3. **Performance** – latency thấp, throughput cao.
4. **Developer Experience** – code dễ đọc, dễ maintain.

> Không hi sinh Correctness hoặc Reliability để tối ưu Performance nếu không có data đo lường cụ thể (không tối ưu sớm).

---

### 10.5. Điều cần TRÁNH tuyệt đối ở production

- `findAll()` không có pagination trên bảng lớn.
- `@Transactional` trên controller.
- HTTP call đồng bộ bên trong vòng lặp (N × latency).
- Hardcode IP/port/secret trong code Java hoặc YAML checked-in.
- Catch `Exception` rồi im lặng không log/rethrow.
- Để mặc định `auto.create.topics.enable=true` trên Kafka production.
- `Thread.sleep()` trong production code.
- Unbounded cache (không có TTL, size limit).

---

File này là "luật chơi backend" cho `PetBoby`.  
Mỗi lần gen/viết code mới, hãy tuân theo các mục trên để code dần tiến gần hơn phong cách Senior DDD/Clean Architecture thực chiến.
