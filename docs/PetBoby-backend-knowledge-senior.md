# BACKEND KNOWLEDGE – MỨC SENIOR

File này bổ sung **độ sâu mức senior** cho từng nhóm kiến thức trong `PetBoby-backend-knowledge-to-code.md`: trade-off, khi nào không nên dùng, lưu ý production, bẫy thường gặp, và cách nhận diện khi review/design. Đọc kèm file knowledge-to-code để biết **code tương ứng trong PetBoby** ở đâu.

---

## 1. IoC / DI / Bean / Scope / Lifecycle – Mức Senior

### 1.1. Constructor injection vs setter/field

- **Trade-off**:
  - **Constructor**: dependency bắt buộc ngay từ lúc tạo bean → không thể có instance “thiếu dependency”. Test dễ: truyền mock vào constructor. Immutable (final field).
  - **Setter/field**: optional dependency hoặc circular (Spring setter có thể break cycle) nhưng dễ quên init, khó thấy dependency đầy đủ, không immutable.
- **Khi nào không nên**:
  - Tránh field injection: khó test, ẩn dependency, không final. Nhiều guideline (Spring, Sonar) khuyến nghị constructor-only.
  - Setter chỉ khi thật sự optional (vd: callback listener) hoặc legacy break circular (ưu tiên refactor thay vì setter).
- **Production / review**:
  - Code review: toàn bộ `*Service` nên có constructor với danh sách dependency rõ ràng; không `@Autowired` trên field.
  - Nếu có > 7–10 dependency trong 1 class → smell: class làm quá nhiều, cân nhắc tách use case hoặc facade.

### 1.2. Bean scope – Senior

- **Singleton**:
  - **Rủi ro**: Nếu bean giữ state (biến instance thay đổi theo request) → race condition, data leak giữa các user. Ví dụ: không được dùng map “userId → cart” trong singleton.
  - **Khi nào dùng**: Stateless service, repository, config. PetBoby: `ProductService`, `OrderRepository` đều stateless → singleton đúng.
- **Prototype**:
  - Mỗi lần inject/lookup tạo instance mới. Tốn memory nếu tạo quá nhiều; thường dùng cho object có vòng đời ngắn, đặc thù từng request (vd: builder, processor per message).
  - PetBoby dùng prototype chỉ trong demo scope; luồng nghiệp vụ chính dùng singleton.
- **Request/Session**:
  - Request: 1 instance/HTTP request. Session: 1 instance/session (stateful). Cẩn thận sticky session khi scale ngang; session timeout và serialization nếu cluster.
- **Review**: Tìm bean có field mutable (List, Map, counter) → kiểm tra scope; nếu singleton thì phải thread-safe hoặc không được lưu state user.

### 1.3. Bean lifecycle – Senior

- **`@PostConstruct`**:
  - Chạy sau khi inject xong, trước khi bean “ready”. Không nên gọi bean khác có thể chưa init xong (thứ tự không đảm bảo giữa các bean). Không làm I/O nặng (block startup).
  - Production: init DB pool, HTTP client thường do framework; nếu tự init (vd: Cloudinary) nên có timeout và fail fast.
- **`BeanPostProcessor`**:
  - Chạy cho mọi bean → logic phải nhanh, không side effect nặng. Spring dùng để tạo proxy (AOP, `@Transactional`). Biết để debug “tại sao proxy không chạy” (thứ tự post-processor).
- **Destroy**:
  - `@PreDestroy` / `DisposableBean`: đóng connection, release resource. Quan trọng khi graceful shutdown (Kubernetes SIGTERM).

### 1.4. Circular dependency – Senior

- **3-level cache (tóm tắt)**:
  - `singletonObjects`: bean đã hoàn thành.
  - `earlySingletonObjects`: bean đã tạo, chưa xong init (để break cycle).
  - `singletonFactories`: factory tạo early reference (có thể trả về proxy).
  - Constructor injection: khi A(B), B(A), không có “early” instance nào tồn tại trước khi constructor chạy xong → Spring không thể inject → fail.
- **Cách xử lý đúng (senior)**:
  - Refactor: tách interface (A phụ thuộc interface I, B implement I và phụ thuộc A); hoặc đưa logic chung sang service thứ 3.
  - Tránh “fix” bằng setter/`@Lazy` nếu chỉ để che circular: che giấu design smell, khó bảo trì.
- **Review**: Build có cảnh báo circular → bắt buộc refactor, không để setter/Lazy lâu dài.

---

## 2. Spring AOP – Mức Senior

### 2.1. Proxy và self-invocation

- **Cơ chế**: Spring tạo proxy (JDK dynamic proxy nếu có interface, CGLIB nếu không). Chỉ **call từ bên ngoài bean** mới đi qua proxy; **gọi this.method()** trong cùng class thì không qua proxy → `@Transactional` / `@Cacheable` / `@Async` không có hiệu lực.
- **Cách xử lý**:
  - Tách method cần transaction/cache sang bean khác, inject và gọi bean đó.
  - Hoặc self-inject (inject chính mình) – dễ gây nhầm lẫn, ưu tiên tách bean.
  - Hoặc dùng `AopContext.currentProxy()` (phải bật `exposeProxy=true`) – ít dùng, khó đọc.
- **Review**: Trong service, mọi method `@Transactional`/`@Cacheable` chỉ nên gọi từ controller hoặc service khác; nếu gọi nội bộ → kiểm tra có bị bypass proxy không.

### 2.2. Điểm cắt (pointcut) và performance

- **Pointcut càng rộng** (vd: `execution(* com.sonnk..*.*(..))`) thì càng nhiều method bị wrap → proxy tạo nhiều, stack sâu. Nên thu hẹp: package + annotation (vd: `@DemoLogged`) hoặc package + tên method.
  - PetBoby: dùng `@Around("@annotation(...DemoLogged)")` → chỉ method có annotation mới bị wrap.
- **`@Around`**:
  - Có quyền gọi `proceed()` hay không, đổi args, đổi return. Nếu quên `proceed()` → method gốc không chạy. Nên dùng try/finally để log thời gian như trong `DemoLoggingAspect`.
- **Aspect order**: Nhiều aspect cùng pointcut → `@Order`. Transaction thường chạy trước (mở transaction) rồi mới tới logging/cache.

### 2.3. Giới hạn Spring AOP

- Chỉ **public** method trên **Spring bean** (không intercept private, không intercept call từ class khác không qua proxy).
- Không intercept constructor, static method.
- Nếu cần weave mọi nơi (kể cả private, library) → AspectJ (compile-time / load-time weaving), phức tạp hơn, ít dùng trong ứng dụng Spring thông thường.

---

## 3. JPA / Hibernate – Mức Senior

### 3.1. Persistence Context và transaction boundary

- **1 transaction = 1 persistence context (mặc định)**. Toàn bộ entity load trong transaction đó được track; khi commit, flush dirty checking. Nếu transaction dài (request-scoped) → PC giữ nhiều entity → tốn memory, risk lazy load ngoài session nếu đóng transaction sớm.
  - **Best practice**: Mở transaction đúng tầm use case (service method), không mở từ filter đến tận controller. Trong PetBoby: `@Transactional` ở service method là đúng.
- **readOnly = true**:
  - Gợi ý cho Hibernate (flush mode, có thể tối ưu). Không “khóa” DB; vẫn có thể ghi nếu code gọi persist/save. Dùng cho query-only method để tránh accidental flush và tối ưu.
- **Review**: Method chỉ đọc thì đánh dấu `@Transactional(readOnly = true)`; method ghi thì `@Transactional` (readOnly = false).

### 3.2. N+1 và fetch strategy – Senior

- **Nhận diện N+1**:
  - Bật `show-sql` (dev), xem log: 1 query list cha + N query con (theo id) → N+1.
  - Hoặc dùng monitoring (query count per request), Datadog/New Relic.
- **Giải pháp** (theo thứ tự ưu tiên):
  1. **Fetch join** (JPQL `join fetch`): 1 query lấy cả cha + con. Hạn chế: nhiều collection fetch cùng lúc có thể sinh Cartesian product → dùng `distinct` (JPQL) hoặc nhiều query (entity graph).
  2. **@EntityGraph**: Khai báo attribute graph, tránh viết JPQL tay.
  3. **Batch size** (`@BatchSize` hoặc `default_batch_fetch_size`): Vẫn N+1 nhưng giảm số query (batch theo 10/20 id). Phù hợp khi không muốn load hết collection trong 1 query.
  4. **Select N+1 chấp nhận được**: Khi N nhỏ (vd: 1 order vài item), đôi khi chấp nhận vài query thay vì query phức tạp.
- **Khi nào không dùng fetch join**:
  - Collection quá lớn (hàng nghìn dòng) → pagination hoặc query riêng, không load hết vào memory.
  - Nhiều collection 1-n cùng lúc (Order có items và payments) → 2 query riêng hoặc DTO/projection thay vì load full entity graph.

### 3.3. Flush / Clear / Merge – Senior

- **Flush**:
  - Đồng bộ PC với DB (INSERT/UPDATE/DELETE chạy), chưa commit. Gọi trước khi native query hoặc khi cần thứ tự (vd: persist rồi dùng ID trong cùng transaction). Flush tự động trước commit và trước một số query (theo flush mode).
  - **Lưu ý**: Flush không đổi state entity; chỉ gửi SQL. Sau flush, PC vẫn managed.
- **Clear**:
  - Detach toàn bộ entity trong PC. Dùng khi muốn “quên” hết để load lại từ DB hoặc tránh memory (sau khi đã convert sang DTO). Sau clear, thay đổi trên object cũ không còn được persist.
  - **Bẫy**: Clear trong transaction rồi vẫn dùng reference cũ → stale data, update không chạy. PetBoby demo `demoMergeAndClear` minh họa đúng.
- **Merge**:
  - Đưa detached entity trở lại PC (tạo bản copy managed, có thể khác instance). Dùng khi nhận entity từ bên ngoài (RPC, deserialize). Merge có thể trigger select + update; với graph sâu cẩn thận cascade.
  - **Senior**: Ưu tiên load-by-id rồi set field từ DTO thay vì merge entity lạ (tránh merge graph không mong muốn).

### 3.4. Soft delete – Senior

- **Query**: Mọi query đọc “còn hiệu lực” phải filter `deletedAt IS NULL`. Nếu quên → leak dữ liệu đã xóa. Có thể dùng `@Where(clause = "deleted_at IS NULL")` trên entity (Hibernate) để tự động thêm điều kiện.
  - **Trade-off**: `@Where` áp dụng mọi nơi; khi cần “xem cả đã xóa” (admin audit) phải dùng native/HQL không qua entity hoặc tạm bỏ filter.
- **Unique constraint**: Cột unique (vd: `code`) phải tính cả bản ghi đã xóa (2 bản ghi cùng code, 1 deleted) → unique partial index (PostgreSQL) hoặc unique trên (code, deleted_at) hoặc code + “deleted_at IS NULL”.

### 3.5. findByIdIn và bulk – Senior

- **IN clause giới hạn**: Một số DB giới hạn số tham số (vd: Oracle ~1000). List id vài chục nghìn → chia batch (vd: 500 id/query) hoặc dùng temporary table.
  - Trong PetBoby: `findByIdIn` dùng trực tiếp; khi list id lớn nên bọc service chia batch.
- **Thứ tự kết quả**: `WHERE id IN (...)` không đảm bảo thứ tự trùng với thứ tự id truyền vào. Nếu cần đúng thứ tự → sort trong memory theo thứ tự id list (Map id → entity rồi duyệt list id).

---

## 4. REST API & Exception handling – Mức Senior

### 4.1. HTTP semantics và versioning

- **Status code**: 200 (OK), 201 (Created + Location), 204 (No Content), 400 (Bad Request), 401 (Unauthorized), 403 (Forbidden), 404 (Not Found), 409 (Conflict), 500 (Internal). Dùng đúng để client và gateway/cache xử lý đúng.
  - **Senior**: Tránh “200 + body error”; không dùng 200 cho lỗi nghiệp vụ. 4xx = client chịu trách nhiệm (retry không giúp nếu không đổi request); 5xx = server, client có thể retry.
- **Versioning**: Có thể qua path (`/api/v1/products`), header, hoặc query. PetBoby chưa version; khi breaking change nên có strategy (vd: v1 deprecated, v2 mới).

### 4.2. GlobalExceptionHandler – Senior

- **Thứ tự handler**: Handler cụ thể (vd: `EntityNotFoundException`) trước; `Exception` cuối. Tránh catch `Exception` quá sớm nuốt lỗi cần xử lý khác.
- **Log**: Trong handler 5xx nên log đầy đủ (stack trace) server-side; response body không nên trả stack trace ra client (lộ thông tin). PetBoby dùng `ApiError` với message là đúng hướng.
- **Validation**: `MethodArgumentNotValidException` (body) và `ConstraintViolationException` (path/query) đều map 400; format lỗi thống nhất (field + message) để FE hiển thị.

### 4.3. DTO và security

- **Không expose entity**: Entity có thể có field nhạy cảm (password hash, internal state). Luôn map sang DTO; không `toJson(entity)` trả thẳng.
  - **Senior**: Khi thêm field entity, kiểm tra có DTO nào serialize entity không; nếu có thì DTO phải chủ động chọn field.
- **Idempotency**: POST tạo resource có thể cần idempotency key (header) để tránh tạo trùng khi retry. PetBoby chưa có; khi làm payment/order nên cân nhắc.

---

## 5. Microservices & Gateway – Mức Senior

### 5.1. Bounded context và coupling

- **User vs Auth**: Auth (login, JWT) tách khỏi User (CRUD profile) để auth có thể scale/security riêng, user service không chứa secret. Auth gọi user (HTTP) để verify credential; không share DB.
  - **PetBoby**: auth service độc lập, sau này gọi user-service; đúng hướng.
- **Order gọi Product**: Order cần giá/tên sản phẩm tại thời điểm đặt. Có thể: (1) gọi product API khi tạo order (sync), (2) copy snapshot (productId, name, price) vào OrderItem. Tránh order phụ thuộc “live” product quá chặt (product xóa/sửa không ảnh hưởng order đã lưu).

### 5.2. Gateway – Senior

- **Single point of failure**: Gateway down → toàn bộ API down. Cần HA (nhiều instance), health check, timeouts không quá lớn.
- **Timeout**: Gateway và client nên có timeout nhỏ hơn hoặc bằng backend. Tránh gateway chờ backend vô hạn → thread pool gateway hết.
- **Filter order**: Authentication filter chạy trước routing; logging/metrics có thể trước hoặc sau. PetBoby mới có AddRequestHeader; sau này thêm auth filter validate JWT trước khi forward.
- **Rate limit / circuit breaker**: Thường đặt ở gateway (hoặc service mesh). PetBoby chưa có; senior cần biết khi nào cần (bảo vệ backend, tránh cascade failure).

### 5.3. Sync vs Async – Senior

- **Sync (REST)**:
  - Ưu: Đơn giản, dễ debug, consistency dễ hiểu. Nhược: Latency cộng dồn; service B chậm thì A chậm; nếu B down thì A fail.
  - **Timeout bắt buộc**: RestTemplate/WebClient phải set connect + read timeout; không để mặc định vô hạn.
- **Async (Kafka/event)**:
  - Ưu: Decouple, throughput cao, B down không block A. Nhược: Eventual consistency, phải thiết kế idempotency, dead letter, monitoring lag.
  - **Senior**: Chọn sync khi cần kết quả ngay (vd: validate giá khi tạo order); chọn async khi có thể chậm (notification, inventory update sau).

---

## 6. Security & JWT – Mức Senior

### 6.1. JWT

- **Secret/key**: HMAC secret (HS256) phải đủ dài, random; không commit vào repo. Prod: lấy từ secret manager (Vault, AWS Secrets Manager). Rotate key thì token cũ hết hiệu lực → có thể dùng short-lived access + refresh token.
  - PetBoby: secret trong config chỉ để demo; production phải đổi.
- **Claim**: `sub` (subject, thường user id), `exp` (expiration), `iat` (issued at). Không đặt dữ liệu nhạy cảm (password, email đầy đủ) vào payload (JWT chỉ base64, không encrypt).
- **Validation**: Backend phải validate chữ ký, `exp`, `iss` (issuer); không tin token từ client mà không verify. Gateway hoặc từng service có thể verify JWT với cùng secret/public key.

### 6.2. Auth flow – Senior

- **Login**: Verify credential (password hash so sánh BCrypt); sau đó mới cấp token. PetBoby auth service hiện demo admin/admin; production phải so sánh hash từ user store (user-service).
  - **Brute force**: Giới hạn số lần login sai (rate limit, lock tài khoản tạm); không log password.
- **Logout**: JWT stateless nên “logout” = client bỏ token. Nếu cần revoke ngay → blacklist token (Redis, TTL = thời gian còn lại của token) hoặc dùng refresh token và revoke refresh token.

---

## 7. Cách dùng file này (Senior)

- **Khi review code**: Đối chiếu từng topic (IoC, AOP, JPA, REST, Gateway, Auth) với mục “Review” / “Production” / “Khi nào không nên” ở trên.
- **Khi thiết kế**: Trade-off và giới hạn giúp chọn option phù hợp (sync vs async, fetch strategy, scope bean).
- **Khi debug**: Hiểu sâu PC, proxy, lifecycle giúp suy luận nhanh (vd: tại sao update không chạy → detached? tại sao @Transactional không rollback → self-invocation?).

File gốc map “knowledge → code” vẫn là `PetBoby-backend-knowledge-to-code.md`; file này bổ sung **độ sâu senior** để không chỉ biết “có gì” mà còn “vì sao, khi nào không, và cẩn thận gì”.
