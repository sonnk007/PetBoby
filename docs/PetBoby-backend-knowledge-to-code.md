## BACKEND KNOWLEDGE → CODE TRONG `PetBoby`

File này tổng hợp **từ đầu đến cuối**: mỗi nhóm kiến thức backend bạn đã học → được áp dụng / demo thế nào trong code `PetBoby`, và **công dụng thực tế** của chúng.

**Cách đọc mỗi mục kiến thức** – mỗi topic được trình bày theo 4 phần để dễ hiểu và nhớ:

| Phần | Nội dung |
|------|----------|
| **Lý thuyết** | Khái niệm là gì, cơ chế hoạt động, tại sao cần. |
| **Cách ứng dụng** | Áp dụng cụ thể ở đâu trong PetBoby (class, config, flow). |
| **Công dụng / Lợi ích** | Dùng để làm gì, mang lại lợi ích gì cho dự án và kiến trúc. |
| **Nhược điểm / Lưu ý** | Hạn chế, trade-off, khi nào không nên lạm dụng, cẩn thận gì. |

**Mức Senior:** Độ sâu trade-off, khi nào không nên dùng, lưu ý production, bẫy thường gặp và cách review → xem **`PetBoby-backend-knowledge-senior.md`** (cùng thư mục).

---

## 1. IoC, DI, Bean, Scope, Bean Lifecycle, Circular Dependency

### 1.1. IoC & Dependency Injection (DI)

- **Lý thuyết**:
  - **IoC (Inversion of Control)**: Thay vì class tự `new` dependency (kiểm soát nằm trong class), một **container** (Spring) tạo và quản lý object, rồi **đưa (inject)** vào class. Kiểm soát “đảo” sang container → gọi là đảo chiều kiểm soát.
  - **DI (Dependency Injection)**: Cách container “đưa” dependency vào class: qua **constructor** (khuyến nghị), **setter**, hoặc **field**. Class không tự tạo dependency, chỉ khai báo cần gì → giảm coupling, dễ thay implementation (vd: mock trong test).
- **Cách ứng dụng trong PetBoby**:
  - `ProductService` dùng **constructor injection**: khai báo tham số `ProductRepository`, `CategoryRepository`, `ProductAppProperties`…; container tạo các bean đó và inject vào khi tạo `ProductService`.
  - `CloudinaryUtil`: `@Component`, inject config bằng `@Value`; container quản lý vòng đời.
- **Công dụng / Lợi ích**:
  - Dễ **thay implementation** (mock repo trong test, đổi config theo môi trường).
  - Giảm `new` thủ công → ít lỗi quản lý vòng đời, code tuân theo SOLID (Dependency Inversion, Interface segregation).
- **Nhược điểm / Lưu ý**:
  - Phải hiểu **scope** (singleton vs prototype): inject nhầm bean có state vào singleton → lỗi concurrent. Nhiều dependency trong một class (vd: > 7–8) → class làm quá nhiều, nên tách use case.

### 1.2. Bean Scope

- **Kiến thức**:
  - `singleton`, `prototype`, `request`, `session`, ... quyết định số lượng instance & vòng đời bean.
- **Code demo**:
  - `ScopedBeans`:
    - `SingletonBean`: @Component mặc định singleton.
    - `PrototypeBean`: @Component + `@Scope("prototype")`.
    - `RequestBean`: @Component + `@Scope(WebApplicationContext.SCOPE_REQUEST)`.
  - Endpoint demo:
    - `/api/demo/scopes` trong `ProductDemoController` trả về `hashCode` + `createdAt` của từng bean → bạn gọi nhiều lần để thấy:
      - singleton: 1 instance cho cả app.
      - prototype: mỗi lần tạo mới.
      - request: mỗi HTTP request một instance.
- **Công dụng thực chiến**:
  - Quyết định **stateless vs stateful** bean.
  - Tránh việc vô tình dùng stateful bean làm singleton gây bug concurrent.

### 1.3. Bean Lifecycle

- **Kiến thức**:
  - Vòng đời: instantiate → populate → *Aware callbacks* → `BeanPostProcessor` → `@PostConstruct` / `init` → bean ready → destroy.
- **Code**:
  - `BaseEntity` (`product`, `order`, `user` module):
    - Sử dụng `@PrePersist`, `@PreUpdate` để tự động set `createdAt`, `updatedAt` trước khi insert/update.
  - `CloudinaryUtil`:
    - `@PostConstruct init()` khởi tạo `Cloudinary` client một lần sau khi DI xong.
  - `ScopedBeans`:
    - `@PostConstruct` log ra khi mỗi bean được tạo.
- **Công dụng**:
  - Đặt **logic init/destroy ở đúng chỗ**, không lẫn với business logic.
  - Tránh phải gọi “init” bằng tay ở nhiều nơi.

### 1.4. Circular Dependency & 3-level cache

- **Kiến thức**:
  - Circular dependency qua constructor injection giữa 2 singleton = lỗi khởi động.
  - Spring dùng 3-level cache để hỗ trợ một số trường hợp, nhưng **constructor-based circular** vẫn fail.
- **Code demo (không chạy runtime)**:
  - `CircularDependencyNotes`:
    - Chứa `AService` ↔ `BService` với constructor injection.
    - TẤT CẢ `@Component/@Autowired` bị comment, có JavaDoc rõ: nếu bật lại sẽ gây lỗi circular.
- **Công dụng**:
  - Nhận diện **design smell**: service phụ thuộc vòng tròn → cần refactor domain / tách interface.
  - Hiểu giới hạn của Spring IoC để tránh debug mất thời gian.

---

## 2. Spring AOP: Aspect, Advice, Proxy, Self-Invocation

### 2.1. Cross-cutting concerns & Advice

- **Lý thuyết**:
  - **Cross-cutting concern**: Logic áp dụng cho **nhiều** class/method (logging, transaction, security, đo thời gian…) nhưng không phải “nghiệp vụ” chính. Nếu viết trực tiếp vào từng method → trùng code, khó bảo trì.
  - **AOP (Aspect-Oriented Programming)**: Tách phần đó ra **Aspect**; Aspect chạy **quanh** (before/after/around) các **point** (vd: method có annotation). **Advice** là đoạn code chạy tại điểm cắt (vd: `@Around` log thời gian). Spring AOP dùng **proxy**: gọi từ bên ngoài bean mới qua proxy → advice mới chạy.
- **Cách ứng dụng trong PetBoby**:
  - `@DemoLogged`: annotation đánh dấu method cần log.
  - `DemoLoggingAspect`: `@Aspect` + `@Component`, `@Around("@annotation(...DemoLogged)")` – log thời gian chạy và gọi `proceed()`. Áp dụng lên các method demo: `demoScopes`, `demoJpaPersistenceContext`, `demoMergeAndClear`, `internalLoggedMethod`.
- **Công dụng / Lợi ích**:
  - Thêm logging, đo performance, trace request ID trên nhiều method mà **không** copy-paste: chỉ gắn annotation (vd: `@DemoLogged`). Transaction (`@Transactional`), cache (`@Cacheable`) bên dưới cũng dùng AOP.
- **Nhược điểm / Lưu ý**:
  - **Self-invocation**: Gọi method có `@Transactional`/`@DemoLogged` **trong cùng class** (this.method()) → không qua proxy → advice **không chạy**. Cần tách sang bean khác hoặc gọi từ bên ngoài. Pointcut quá rộng → nhiều method bị wrap → ảnh hưởng performance.

### 2.2. Proxy & self-invocation

- **Kiến thức**:
  - Spring AOP hoạt động dựa trên **proxy**.
  - Gọi method annotated từ **bên ngoài bean** → qua proxy → advice chạy.
  - Gọi **nội bộ cùng class** (self-invocation) → không qua proxy → advice **không chạy**.
- **Code demo**:
  - Trong `ProductDemoService`:
    - `selfInvocationEntry()` gọi `internalLoggedMethod()` có `@DemoLogged`.
    - JavaDoc ghi rõ: self-invocation → `DemoLoggingAspect` không log.
    - Method không được expose qua controller, chỉ dùng khi test.
- **Công dụng**:
  - Tránh hiểu nhầm khi dùng `@Transactional/@Cacheable/@Async` — nếu gọi nội bộ thì không có tác dụng.
  - Khi cần, bạn biết phải **tách method sang bean khác** hoặc lấy reference proxy từ context.

---

## 3. JPA / Hibernate: ORM, Persistence Context, Dirty Checking, Flush, Clear, Merge

### 3.1. Persistence Context & Dirty Checking trong business code

- **Kiến thức**:
  - Managed entity được theo dõi, dirty checking so sánh snapshot khi flush.
  - Không cần gọi `save()` lại nếu entity đang managed trong transaction.
- **Code thực chiến**:
  - `ProductService.updateProductBasicInfo`:
    - Annotated `@Transactional`.
    - Load `Product` → set name/description → **KHÔNG** gọi `save()`.
    - Khi transaction commit, JPA flush UPDATE dựa trên state mới.
- **Công dụng**:
  - Code sạch hơn, ít câu lệnh repository thừa.
  - Giảm rủi ro quên `save()` ở một vài nơi (nếu transaction design đúng).

### 3.2. Demo JPA state, flush, clear, merge

- **Kiến thức**:
  - State: Transient → Managed → Detached → Removed.
  - `clear()` làm entity detached, thay đổi sau đó không được flush.
  - `merge()` tạo managed copy mới từ detached entity.
- **Code demo**:
  - `demoJpaPersistenceContext(Long productId)`:
    - Nếu `productId == null`:
      - Tạo `Product` mới (`transient`), `save()` → insert khi flush.
    - Nếu có `productId`:
      - Load managed `Product`, đổi name, không `save()` → dirty checking update.
  - `demoMergeAndClear(Long productId)`:
    - Load managed entity, log state.
    - `entityManager.clear()` → entity detached.
    - Thay đổi name trên detached object → không flush.
    - `entityManager.merge(detached)` → tạo managed copy `merged`.
    - Commit → flush UPDATE từ `merged`.
- **Công dụng**:
  - Hiểu chính xác **khi nào SQL chạy**, tránh bug khó chịu (update không chạy, hoặc chạy quá nhiều).
  - Thiết kế transaction boundary hợp lý trong service.

### 3.3. N+1 Query & Cách Tránh Bằng Fetch Join

- **Kiến thức**:
  - N+1 xảy ra khi:
    - Bạn load danh sách entity cha (1 query).
    - Sau đó trong code Java lặp và truy cập collection LAZY (con) → mỗi lần truy cập bắn thêm 1 query.
  - Hậu quả:
    - Số query tăng theo số bản ghi (N+1), rất tốn thời gian khi data lớn.
  - Cách tránh phổ biến:
    - Dùng **fetch join** trong JPQL/HQL (`join fetch`) hoặc `@EntityGraph`.
- **Code demo trong `order` module**:
  - `Order`, `OrderItem`, `OrderItemTopping`:
    - Quan hệ 1-n đều dùng `FetchType.LAZY` → dễ tạo N+1 nếu không cẩn thận.
  - Repository:
    - `OrderRepository.findByBranchCodeAndCreatedAtBetween(...)`:
      - Chỉ load bảng `orders`, không fetch kèm items/toppings.
    - `OrderRepository.findWithItemsAndToppingsByBranchCodeAndCreatedAtBetween(...)`:
      - Dùng JPQL với `left join fetch o.items i left join fetch i.toppings t` để load toàn bộ graph.
  - Service DEMO (không vào luồng chính):
    - `OrderNPlusOneDemoService`:
      - `loadOrdersNaive(...)`:
        - Gọi `findByBranchCodeAndCreatedAtBetween` → trả về danh sách `Order`.
        - Lặp qua `order.getItems()` và `item.getToppings()` → kích hoạt N+1 query.
      - `loadOrdersOptimized(...)`:
        - Gọi `findWithItemsAndToppingsByBranchCodeAndCreatedAtBetween` (fetch join) → giảm số query.
  - Controller DEMO:
    - `OrderNPlusOneDemoController`:
      - `GET /api/demo/orders/nplus1/naive`
      - `GET /api/demo/orders/nplus1/optimized`
      - Trả về DTO `OrderSummary` (orderId, orderCode, branchCode, itemCount, toppingCount) cho dễ so sánh.
- **Cách tự quan sát & học**:
  - Đảm bảo trong `order` có `spring.jpa.show-sql: true` (đã bật).
  - Gọi:
    - `GET /api/demo/orders/nplus1/naive?branchCode=HN-CauGiay-01&from=2025-01-01T00:00:00&to=2025-12-31T23:59:59`
    - `GET /api/demo/orders/nplus1/optimized?branchCode=HN-CauGiay-01&from=2025-01-01T00:00:00&to=2025-12-31T23:59:59`
  - So sánh:
    - Số lượng query SQL in ra.
    - Thời gian response (khi data lớn).
- **Công dụng thực chiến**:
  - Nhận diện pattern N+1 khi review code: lặp trên list entity, gọi getter trên collection LAZY bên trong.
  - Biết cách xử lý:
    - Dùng fetch join / entity graph / batch-size.
    - Thiết kế API tránh trả về graph quá sâu không cần thiết.

### 3.4. Entity Mapping, Soft Delete, Enum & JSON Field

- **Kiến thức**:
  - Mapping cơ bản:
    - `@Entity`, `@Table`, `@Id`, `@GeneratedValue`.
    - `@ManyToOne`, `@OneToMany`, `@OneToOne`, `fetch = LAZY`.
  - Kế thừa & `@MappedSuperclass`:
    - Dùng `@MappedSuperclass` cho các trường audit chung.
    - Dùng `@Inheritance` cho hierarchy user (customer/employee).
  - Soft delete:
    - Thay vì `DELETE` cứng, set `deletedAt`, `deletedBy`.
  - Enum & JSON:
    - Enum map bằng `@Enumerated(EnumType.STRING)` để dễ đọc và bền schema.
    - Trường JSON (string) để lưu cấu trúc linh hoạt (vd: toppings).
- **Code**:
  - `BaseEntity` trong `product`, `order`, `user`:
    - `@MappedSuperclass` với các field:
      - `createdAt`, `updatedAt`, `deletedAt`, `deletedBy`.
    - `@PrePersist` / `@PreUpdate` set tự động `createdAt`/`updatedAt`.
    - `markDeleted(String user)` set `deletedAt`, `deletedBy` → soft delete.
  - Soft delete thực chiến:
    - `ProductService.softDelete(Long id, String deletedBy)`:
      - Gọi `getProductOrThrow(id)` → lấy entity managed.
      - Gọi `product.markDeleted(deletedBy)` → **không** xóa record khỏi DB, chỉ đánh dấu.
  - **Senior (đã áp dụng)**: Entity-level filter soft delete: `Product`, `Category`, `Topping`, `Order` dùng `@SQLRestriction("deleted_at IS NULL")` (Hibernate 6.3+) để mọi query qua entity tự loại bản ghi đã xóa; khi cần xem cả đã xóa (audit) dùng native/HQL riêng.
  - Inheritance & quan hệ:
    - `UserModel`:
      - `@Inheritance(strategy = InheritanceType.JOINED)` → tách bảng `users`, `customer`, `employee` nhưng join bằng cùng `id`.
      - `@OneToOne` với `ProfileModel`.
    - `Order` – `OrderItem` – `OrderItemTopping`:
      - `@OneToMany` từ `Order` sang `OrderItem` (LAZY, cascade ALL, orphanRemoval).
      - `@OneToMany` từ `OrderItem` sang `OrderItemTopping` (LAZY).
  - Enum:
    - `ProductStatus`, `OrderStatus`, `PaymentMethod`, `UserRole`, `UserStatus`, `EmploymentType`:
      - Dùng `@Enumerated(EnumType.STRING)` ở các entity (`Product`, `Topping`, `Order`, `UserModel`, `EmployeeModel`, ...).
  - JSON field:
    - `Product.toppings`:
      - Trường `String toppings` lưu JSON.
      - Getter/setter dùng Jackson (`ObjectMapper`) để map `List<ToppingInfo> ↔ String`.
- **Công dụng thực chiến**:
  - BaseEntity + soft delete:
    - Giảm lặp code audit, dễ filter dữ liệu “chưa xóa”.
    - Không mất lịch sử record (phù hợp hệ thống tài chính, HR).
  - Inheritance JOINED cho user:
    - Tách rõ logic & field của `CustomerModel` vs `EmployeeModel` nhưng vẫn share identity.
  - Enum STRING:
    - Dễ đọc trong DB, tránh lỗi khi thay đổi thứ tự enum.
  - JSON field cho toppings:
    - Linh hoạt khi schema topping thay đổi nhỏ, không phải normalize quá mạnh nhưng vẫn có type-safe ở Java.

### 3.5. SELECT IN (findByIdIn) – Load nhiều entity trong 1 query

- **Kiến thức**:
  - Khi cần load nhiều entity theo danh sách id, thay vì:
    - Vòng lặp gọi `findById(id)` từng cái → nhiều query nhỏ (có thể gây N+1 kiểu khác).
  - Nên dùng **IN**: `WHERE id IN (…)` → 1 query lấy toàn bộ.
  - Spring Data JPA hỗ trợ qua derived method `findByIdIn(Collection<Long> ids)`.
- **Code trong `product` module**:
  - Repository:
    - `ProductRepository.findByIdIn(List<Long> ids)`:
      - IdIn → WHERE id IN (…): lấy nhiều product theo danh sách id truyền vào.
      - Dùng khi các service khác (vd: order) cần resolve nhiều productId.
  - Service:
    - `ProductService.getByIds(List<Long> ids)`:
      - Gọi `productRepository.findByIdIn(ids)` trong 1 query.
  - API demo:
    - `GET /api/products/bulk?ids=1,2,3` trong `ProductController`:
      - Nhận `ids` dạng query param list → gọi `getByIds` → trả về danh sách `ProductResponse`.
- **Công dụng thực chiến**:
  - Tránh vòng lặp `findById` gây nhiều query.
  - Là pattern cơ bản khi cần bulk load entity theo nhiều id (phù hợp cho các use case như order, báo cáo, batch job).
- **Senior (đã áp dụng)**: `ProductService.getByIds` chia batch 500 id/query khi list lớn (tránh giới hạn IN của DB); `application.yml` (product, order) có `default_batch_fetch_size: 50` để giảm N+1 khi lazy load collection. Handler 5xx (product, order, auth): log đầy đủ server-side, response chỉ trả message chung "Internal server error".

---

## 4. Spring Boot Fundamentals: Auto-config, Profiles, ConfigurationProperties

### 4.1. Auto-configuration & @SpringBootApplication

- **Kiến thức**:
  - `@SpringBootApplication` = `@Configuration + @EnableAutoConfiguration + @ComponentScan`.
  - Spring Boot auto-config dựa vào dependencies (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, …) và `application.yml`.
- **Code**:
  - `ProductApplication`:
    - Entry point module `product`, bật auto-config cho web, JPA, datasource, logging,...
- **Công dụng**:
  - Giảm cấu hình tay (XML/Java config), tập trung vào business code.

### 4.2. Externalized Configuration với @ConfigurationProperties

- Đã phân tích ở trên:
  - `ProductAppProperties` map `petboby.product.*`.
  - Dùng trong `ProductService` để quyết định page size.
- **Giá trị**:
  - Config **type-safe**, một chỗ, dễ quản lý theo environment.

### 4.3. Profiles & application-*.yml

- **Kiến thức**:
  - Profiles cho phép cấu hình khác nhau theo môi trường (`dev`, `prod`, ...).
- **Code**:
  - `application-dev.yml`:
    - `spring.config.activate.on-profile: dev`.
    - Bật `show-sql`, `format_sql`, set logging level, override `petboby.product.default-page-size`.
- **Công dụng**:
  - Dev: log nhiều để debug, test paging nhỏ.
  - Prod: có thể dùng profile khác (sau này) với cấu hình DB thật, logging tối ưu.

---

## 5. User / Order / Domain Models & Business Mapping

### 5.1. Domain models gắn với nghiệp vụ chuỗi cà phê / trà sữa

- **Product/Menu**:
  - `Category`, `Product`, `Topping` structure phản ánh:
    - Nhóm đồ uống, mã sản phẩm, size, trạng thái, topping kèm.
- **Order**:
  - `Order`, `OrderItem`, `OrderItemTopping`, `OrderStatus`, `PaymentMethod`:
    - Chuẩn hoá dữ liệu để tính doanh thu, COGS, báo cáo chi nhánh.
- **User/HR**:
  - `UserModel` + `CustomerModel` + `EmployeeModel` + `ProfileModel` + enums `UserRole`, `UserStatus`, `EmploymentType`:
    - Phục vụ auth/role + quản lý hồ sơ nhân viên + lương/hợp đồng.
- **Công dụng**:
  - Đây là “xương sống” domain, để sau này apply kiến thức system design, microservices, Kafka, Redis, reporting lên trên.

---

## 6. Microservices 101 & Kiến trúc PetBoby (Phase 2 – Tháng 4, Tuần 1)

- **Monolith vs Microservices**: Monolith = một app/DB/process; Microservices = nhiều service nhỏ, độc lập, giao tiếp API. PetBoby đã tách user/product/order/gateway; client gọi gateway (8080), gateway route theo path.
- **Bounded context**: user = identity/auth/profile; product = category/product/topping/menu; order = đơn/item/thanh toán; gateway = routing.
- **Sync vs Async**: Hai cách service nói chuyện với nhau. **Sync** = gọi-chờ (Order→Product qua RestTemplate, Tuần 3); **Async** = gửi event vào broker, ai cần thì subscribe (Kafka, Tuần 4). PetBoby dùng **cả hai** trong cùng luồng tạo đơn → chi tiết và khi nào dùng cái gì: **mục 6.2.0**.
- **Gateway**: `gateway-service/src/main/resources/application.yml` — `/api/auth/**` → 8084, `/api/users/**` → 8081, `/api/products/**` → 8082, `/api/orders/**` và `/api/demo/orders/**` → 8083. Chi tiết: `backend-overview.md`.

### 6.1. API Gateway – Routing & Filter (Phase 2 – Tuần 2)

- **Kiến thức**:
  - **Routing**: Gateway nhận request theo path (predicate `Path=...`), forward đến backend (uri). Client chỉ cần biết một host (gateway).
  - **Filter**: Cross-cutting (thêm header, log, auth) áp dụng trước/sau khi forward. Ví dụ: `AddRequestHeader` để backend biết request đi qua gateway.
- **Code/config**:
  - Route order: `Path=/api/orders/**` và `Path=/api/demo/orders/**` → `http://localhost:8083`.
  - Filter: `AddRequestHeader=X-Gateway-Route, order-service` (và `order-demo`) trên từng route.
  - Order service: `server.port: 8083` trong `order/src/main/resources/application.yml` để tránh trùng cổng với gateway (8080).
- **Công dụng**: Một cửa cho client; dễ thêm auth/log/rate-limit sau; backend nhận header để log hoặc kiểm tra nguồn.

### 6.2.0. Sync vs Async – Lý thuyết và tại sao PetBoby dùng cả hai

*(Phần này trả lời câu hỏi: “Có luồng sync rồi, async là gì và khi nào dùng?” – nên đọc **trước** 6.2 RestTemplate và **trước** mục 7 Kafka để nắm bức tranh chung.)*

#### Lý thuyết: Sync và Async khác nhau thế nào?

| | **Đồng bộ (Sync)** | **Bất đồng bộ (Async)** |
|---|-------------------|-------------------------|
| **Cách giao tiếp** | A gửi request → **chờ** B xử lý xong → nhận response. A **block** cho đến khi có kết quả. | A gửi message vào **kênh** (broker, queue). B (và C, D…) **tự lấy** message từ kênh và xử lý. A **không chờ** B. |
| **A có biết kết quả ngay không?** | Có. Thành công hay lỗi (timeout, 4xx, 5xx) A đều biết ngay trong request đó. | Không. A chỉ biết “đã gửi vào kênh”. B có nhận, xử lý thành công hay lỗi, A không biết trong luồng gọi. |
| **Ví dụ kỹ thuật** | HTTP REST: Order gọi `GET /api/products/bulk?ids=...` → chờ Product trả JSON. | Message broker (Kafka): Order gửi event vào topic `order.created` → Product (và service khác) subscribe topic, tự đọc và xử lý. |
| **Độ phức tạp** | Đơn giản: một request – một response. Debug dễ (trace theo request). | Phức tạp hơn: cần broker, topic, consumer group, idempotency, eventual consistency. |

- **Sync** phù hợp khi: Bạn **cần kết quả ngay** trong cùng luồng (vd: lấy giá sản phẩm để tính tiền đơn, validate user tồn tại). Nếu B chậm hoặc down → A chậm hoặc lỗi theo.
- **Async** phù hợp khi: Bạn **không cần chờ** kết quả; chỉ cần “thông báo” để ai đó xử lý sau (vd: đơn đã tạo → gửi email, cập nhật thống kê, ghi audit). A trả response cho client nhanh; B xử lý độc lập, có thể chậm hoặc retry.

#### Tại sao lộ trình dạy Sync (Tuần 3) trước, Async (Tuần 4) sau?

- **Sync dễ hiểu hơn**: Một request, một response, dòng chảy rõ. RestTemplate gọi HTTP quen thuộc. Hợp để bắt đầu “service gọi service”.
- **Async cần thêm khái niệm**: Broker, topic, consumer, at-least-once, idempotency. Dạy sau khi đã có “luồng sync” giúp bạn so sánh: “Trước đó Order **chờ** Product; giờ Order **gửi event** rồi thôi, Product tự xử lý.”
- **Trong thực tế cả hai cùng tồn tại**: Một use case có thể vừa dùng sync (chỗ cần kết quả ngay) vừa dùng async (chỗ chỉ thông báo). PetBoby minh họa đúng điều đó.

#### Trong PetBoby, cùng một luồng “tạo đơn” dùng cả Sync và Async

1. **Sync (Tuần 3 – RestTemplate)**  
   Order **cần ngay** giá và tên sản phẩm để ghi vào OrderItem và lưu DB. Nếu không có → không tính được tiền, không tạo đơn được.  
   → Order gọi Product: `getProductsByIds(ids)` (HTTP), **chờ** response, rồi mới tính total và lưu Order. Đây là **sync**.

2. **Async (Tuần 4 – Kafka)**  
   Sau khi đơn đã lưu, Order chỉ cần **thông báo** “đơn vừa tạo” để:
   - Product service log / audit / (sau này) cập nhật tồn kho ảo;
   - (Sau này) Notification service gửi email, Loyalty service cộng điểm…  
   Order **không cần chờ** các việc đó. Chỉ cần gửi event lên Kafka rồi trả response cho client.  
   → Order gọi `orderEventPublisher.publishOrderCreated(...)` (fire-and-forget). Đây là **async**.

**Tóm lại**: Sync = “cần kết quả ngay trong luồng này” (lấy giá → tạo đơn). Async = “chỉ cần thông báo, ai cần thì xử lý sau” (đơn đã tạo → log, notify, analytics…). PetBoby cố ý dùng **cả hai** trong cùng use case để bạn thấy khi nào dùng sync, khi nào dùng async.

#### Công dụng / Lợi ích khi hiểu rõ Sync vs Async

- Thiết kế API và luồng nghiệp vụ đúng: chỗ nào bắt buộc có dữ liệu ngay thì sync; chỗ nào “làm sau cũng được” thì async để không kéo chậm response.
- Tránh nhầm lẫn: không dùng async cho “validate product tồn tại” (cần kết quả ngay để quyết định có tạo đơn hay không); không dùng sync cho “gửi email thông báo” (không cần chờ email gửi xong mới trả response).

#### Nhược điểm / Lưu ý

- Nếu chỉ dùng sync cho mọi thứ: một service chậm (vd: gửi email) sẽ làm cả luồng tạo đơn chậm; client chờ lâu.
- Nếu dùng async cho cả “lấy giá”: Order gửi event “tôi cần giá product 1,2,3” rồi trả response ngay → client không có total, không có đơn hoàn chỉnh; phải thiết kế lại (vd: client gọi lại sau, hoặc dùng sync cho bước lấy giá).

---

### 6.2. Synchronous communication – RestTemplate (Tuần 3)

*(So sánh Sync vs Async và khi nào dùng từng loại: **mục 6.2.0**. Phần async (Kafka) tương ứng: **mục 7**.)*

#### Lý thuyết

- **Giao tiếp đồng bộ (sync) giữa hai service**: Service A gửi HTTP request tới service B, **chờ** B xử lý và trả response. A biết ngay kết quả (thành công hay lỗi). So với Kafka (bất đồng bộ), sync đơn giản hơn nhưng A phụ thuộc B **đang chạy và phản hồi đủ nhanh**.
- **RestTemplate**: Là client HTTP của Spring (blocking) để gọi REST API: GET, POST, … Bạn cấu hình **base URL** (hoặc URL đầy đủ), **timeout** (connect timeout, read timeout), gọi `getForObject`, `exchange`, … và nhận response (object hoặc ResponseEntity). Nếu server trả 4xx/5xx, RestTemplate ném exception (vd: `HttpClientErrorException.NotFound`).
- **Timeout**: **Connect timeout** – thời gian chờ thiết lập kết nối (server không phản hồi → fail nhanh). **Read timeout** – thời gian chờ đọc response (server xử lý lâu → tránh block vô hạn). Thiếu timeout → một service chậm có thể “kéo chết” service gọi nó.

#### Cách ứng dụng trong PetBoby

- **Order gọi Product** khi tạo đơn: Order cần **giá và tên sản phẩm** để ghi vào OrderItem (snapshot). Thay vì client gửi sẵn giá (dễ giả mạo), Order service **tự gọi** Product service để lấy thông tin theo `productId`.
- **Port (interface)**: `ProductClient` (application/port) – hai method `getProductById(Long id)` và `getProductsByIds(List<Long> ids)`. Order service chỉ phụ thuộc interface, không biết bên dưới là HTTP hay gì.
- **Implementation**: `ProductRestClient` (infrastructure/client) – inject `RestTemplate` và `ProductServiceProperties` (base URL, timeout). `getProductById` → GET `{baseUrl}/api/products/{id}`; `getProductsByIds` → GET `{baseUrl}/api/products/bulk?ids=1,2,3`. 404 → trả `Optional.empty()`; timeout hoặc 5xx → ném `ProductServiceUnavailableException` (map sang 503).
- **Config**: `ProductServiceProperties` (`petboby.order.product-service.base-url`, `connect-timeout-ms`, `read-timeout-ms`), `RestTemplateConfig` tạo bean `RestTemplate` với timeout từ properties. Order `application.yml` có đủ các key trên.
- **Flow**: `OrderService.createOrder` lấy danh sách `productId` từ request → gọi `productClient.getProductsByIds(ids)` **một lần** (bulk) → validate tất cả id tồn tại → dùng kết quả để set tên, giá cho từng OrderItem rồi lưu Order.

#### Công dụng / Lợi ích

- **Một nguồn sự thật**: Giá và tên sản phẩm luôn lấy từ Product service, không do client truyền lên → tránh gian lận, đảm bảo snapshot đúng tại thời điểm tạo đơn.
- **Bulk thay vì N lần gọi**: Dùng `getProductsByIds(ids)` thay vì gọi `getProductById` từng id trong vòng lặp → **1 HTTP request** thay vì N request → nhanh hơn, ít tải hơn.
- **Tách dependency qua port**: Order phụ thuộc `ProductClient` (interface); implementation có thể đổi (RestTemplate, WebClient, stub trong test) mà không đổi use case.
- **Timeout rõ ràng**: Cấu hình connect/read timeout tránh Order bị treo khi Product chậm hoặc down.

#### Nhược điểm / Lưu ý

- **Coupling thời gian**: Order **phải chờ** Product trả lời. Product chậm hoặc down → tạo đơn chậm hoặc lỗi (503). Cần có chiến lược retry/fallback hoặc chấp nhận “khi Product down thì không tạo được đơn”.
- **RestTemplate blocking**: Mỗi lần gọi tốn một thread đang chờ I/O. Gọi nhiều service đồng thời hoặc nhiều request cùng lúc có thể cần thread pool lớn; có thể cân nhắc WebClient (reactive) cho high throughput.
- **Network và versioning**: Product API đổi contract (field, URL) → Order phải cập nhật (DTO, URL). Nên có version API hoặc contract test để phát hiện sớm.

---

## 7. Kafka – Event-driven (Phase 2 – Tháng 4, Tuần 4)

*(Đây là phần **async** tương ứng với sync (RestTemplate) ở mục 6.2. Khi nào dùng sync, khi nào dùng async trong cùng một luồng: **mục 6.2.0**.)*

### 7.1. Lý thuyết

- **Event-driven là gì?**  
  Thay vì service A **gọi trực tiếp** service B qua HTTP (đồng bộ: A gửi request, chờ B trả lời), A chỉ **gửi một thông báo (event)** vào một “kênh” (message broker). Service B (và có thể C, D…) **đăng ký lắng nghe** kênh đó và tự xử lý khi nhận được message. A không cần biết B có nhận hay không, không chờ kết quả từ B.

- **Kafka trong vai trò đó:**  
  Kafka là **message broker**: lưu message theo **topic** (tên kênh, ví dụ `order.created`). **Producer** (order service) gửi message vào topic; **Consumer** (product service, notification service…) subscribe topic và kéo message về xử lý. Message được lưu trên đĩa, có thể đọc lại (replay), và nhiều consumer có thể đọc cùng topic.

- **Các khái niệm cốt lõi:**
  - **Topic**: Tên kênh (vd: `order.created`). Một topic có thể có nhiều **partition** để tăng throughput; message có **key** sẽ đi vào cùng partition (thứ tự theo key được bảo toàn trong partition).
  - **Consumer group**: Nhiều instance consumer cùng tên group **chia nhau** đọc message: mỗi message chỉ được **một** consumer trong group xử lý. Giúp scale ngang: thêm instance là thêm sức xử lý mà không xử lý trùng message.
  - **Eventual consistency**: Dữ liệu giữa các service không đồng bộ ngay lập tức. Order đã lưu DB, nhưng product/notification nhận event “sau một chút”. Hệ thống chấp nhận “cuối cùng sẽ đồng bộ” thay vì “đồng bộ ngay”.

- **At-least-once delivery**: Kafka đảm bảo message **ít nhất được giao một lần**. Trong một số tình huống (retry, restart) consumer có thể **nhận lại cùng message**. Nên logic xử lý phải **idempotent**: xử lý hai lần cùng một event cho cùng orderId vẫn cho kết quả đúng (vd: “cập nhật tồn kho cho order X” chạy 2 lần vẫn không trừ tồn kho 2 lần).

### 7.2. Cách ứng dụng trong PetBoby

- **Order service (producer – người gửi event):**
  - **Event**: `OrderCreatedEvent` (application/event) – chứa orderId, orderCode, branchCode, customerId, totalAmount, finalAmount, createdAt. Đây là payload gửi lên Kafka dưới dạng JSON.
  - **Port**: `OrderEventPublisher` (application/port) – interface `publishOrderCreated(OrderCreatedEvent)`. Order service chỉ gọi port, không biết bên dưới là Kafka hay queue khác.
  - **Implementation**: `KafkaOrderEventPublisher` (infrastructure/messaging) – inject `KafkaTemplate`, gọi `kafkaTemplate.send(topicOrderCreated, key, event)`. Topic lấy từ config `petboby.order.kafka.topic-order-created` (vd: `order.created`).
  - **Config**: `KafkaProducerConfig` tạo `KafkaTemplate` với `JsonSerializer` cho value; `application.yml`: `spring.kafka.bootstrap-servers`, `spring.kafka.producer.value-serializer: JsonSerializer`.
  - **Thời điểm gửi**: Trong `OrderService.createOrder`, **sau khi** `orderRepository.save(order)` thành công, gọi `orderEventPublisher.publishOrderCreated(...)`. Gửi xong không chờ kết quả (fire-and-forget).

- **Product service (consumer – người nhận event):**
  - **Payload**: `OrderCreatedPayload` (infrastructure/messaging) – record có **cùng tên field** và kiểu dữ liệu với `OrderCreatedEvent` để JSON deserialize đúng (orderId, orderCode, branchCode, totalAmount, finalAmount, createdAt).
  - **Listener**: `OrderCreatedKafkaListener` – method có `@KafkaListener(topics = "order.created", groupId = "product-service")`, tham số là `OrderCreatedPayload`. Hiện tại trong method **chỉ log** (sau có thể: ghi audit, cập nhật tồn kho ảo, gửi notification).
  - **Config**: `application.yml` – `spring.kafka.consumer.group-id: product-service`, `value-deserializer: JsonDeserializer`, `spring.json.value.default.type` trỏ tới class `OrderCreatedPayload`, `spring.json.trusted.packages: "*"` (để deserialize JSON vào class trong package của app).

### 7.3. Công dụng / Lợi ích

- **Tách ràng buộc giữa các service**: Order không cần gọi HTTP tới product hay notification khi tạo đơn. Chỉ cần “thông báo đơn đã tạo” lên topic; ai cần thì subscribe. Thêm service mới (vd: loyalty, analytics) chỉ cần thêm consumer, không sửa order.
- **Chịu tải tốt hơn**: Order trả response cho client ngay sau khi lưu DB và gửi event; không chờ product/notification xử lý xong. Kafka lưu message, consumer xử lý với tốc độ của mình.
- **Scale consumer**: Chạy thêm nhiều instance product (cùng consumer group) → message được chia đều, throughput tăng; mỗi message vẫn chỉ một instance xử lý.
- **Có “lịch sử” event**: Message lưu trên Kafka một thời gian; có thể replay (đọc lại) để sửa lỗi hoặc build lại dữ liệu.

### 7.4. Nhược điểm / Lưu ý

- **Eventual consistency**: Dữ liệu giữa order DB và product/notification **không đồng bộ ngay**. Nếu cần “đọc ngay sau khi tạo đơn” mà dữ liệu phụ thuộc consumer (vd: số điểm thưởng) thì phải thiết kế UX/API phù hợp (vd: hiển thị “đang xử lý”).
- **Có thể nhận trùng message (at-least-once)**: Consumer phải **idempotent**. Ví dụ: cập nhật tồn kho theo orderId – nếu nhận 2 lần cùng event thì dùng “trừ tồn kho cho orderId X” chỉ một lần (vd: check “orderId X đã xử lý chưa” trước khi trừ).
- **Vận hành thêm một hệ thống**: Cần chạy Kafka (cluster), monitor lag, disk, consumer chết. Kafka down hoặc network lỗi → producer có thể fail hoặc message gửi chậm; cần có chiến lược retry/error handling.
- **Thiết kế event và schema**: Payload (OrderCreatedEvent) thay đổi (thêm/xóa field) có thể làm consumer cũ lỗi. Nên có version event hoặc backward-compatible schema (consumer bỏ qua field không biết).

### 7.5. Quản lý topic, consumer, offset và log request/message

*(Tài liệu thao tác chi tiết: **`kafka-operations.md`**. Phần dưới tóm tắt lý thuyết và cách áp dụng trong PetBoby.)*

#### Lý thuyết

- **Topic**: Tên kênh (vd: `order.created`). Có thể có nhiều **partition** (tăng partition → tăng throughput; message cùng key vào cùng partition). **Replication factor** = số bản copy partition (cluster). Quản lý topic = tạo, list, describe (partition, replication); có thể tạo bằng CLI (`kafka-topics.sh`) hoặc từ ứng dụng (KafkaAdmin, `NewTopic`).
- **Consumer / Consumer group**: Mỗi consumer thuộc một **group** (`group.id`). Các instance cùng group **chia nhau** đọc partition (mỗi partition chỉ gán cho một consumer trong group). Quản lý consumer = xem group nào đang tồn tại, group đó đang đọc partition nào, **offset** đã commit và **lag** (số message chưa xử lý). Lệnh: `kafka-consumer-groups.sh --list`, `--describe --group <name>`.
- **Offset**: Mỗi message trong partition có **offset** (số thứ tự). Consumer đọc theo offset; sau khi xử lý (hoặc theo chu kỳ) **commit offset** để lần sau không đọc lại. **Commit strategy**: auto (theo chu kỳ, đơn giản nhưng có thể mất/trùng message) vs manual (commit sau khi xử lý xong → at-least-once, cần idempotent). **Reset offset**: khi cần đọc lại từ đầu hoặc từ một thời điểm (consumer phải dừng khi reset).
- **Log request / message**: Producer nên log **topic, key, payload (summary)** trước/sau khi gửi; consumer nên log **topic, partition, offset, key, payload (summary)** khi nhận. Giúp trace, debug, audit. Production tránh log full payload nếu có dữ liệu nhạy cảm; có thể dùng log level (DEBUG = chi tiết, INFO = summary).

#### Cách quản lý (tóm tắt)

| Mục | Cách làm |
|-----|----------|
| **Topic** | Tạo: `kafka-topics.sh --create --topic order.created --partitions 2 --replication-factor 1`. List: `--list`. Chi tiết: `--describe --topic order.created`. |
| **Consumer group** | List group: `kafka-consumer-groups.sh --list`. Chi tiết (offset, lag): `kafka-consumer-groups.sh --describe --group product-service`. |
| **Offset** | Xem trong output `--describe --group`. Reset: `--reset-offsets --to-earliest` (hoặc `--to-latest`) cho topic, **--execute**. Consumer nên dừng khi reset. |
| **Log message** | Producer: log topic, key, orderId/orderCode trước/sau send; callback log partition/offset khi thành công. Consumer: nhận `ConsumerRecord` để lấy partition, offset; log topic, partition, offset, key, orderId/orderCode (payload summary). |

#### Cách ứng dụng trong PetBoby

- **Topic**: Hiện tại dùng topic `order.created`. Có thể tạo sẵn bằng script (xem `docs/kafka-operations.md`) hoặc thêm bean `NewTopic` trong Order hoặc Product module (Spring Boot khởi động sẽ tạo topic nếu broker cho phép auto-create). Ví dụ:
  ```java
  @Bean
  public NewTopic orderCreatedTopic() {
    return TopicBuilder.name("order.created").partitions(2).replicas(1).build();
  }
  ```
  (Cần `KafkaAdmin` trong context; Spring Boot auto-config thường đã có khi có `spring.kafka.bootstrap-servers`.)
- **Consumer group**: Trong PetBoby product service dùng `groupId = "product-service"`. Xem group và offset/lag bằng lệnh `kafka-consumer-groups.sh --describe --group product-service` (khi Kafka đang chạy).
- **Offset**: Spring Kafka mặc định **auto commit**. Giữ nguyên cho demo; khi cần “xử lý xong mới commit” có thể tắt auto commit và dùng `Acknowledgment` trong listener (xem Spring Kafka doc).
- **Log request / message**:
  - **Producer** (`KafkaOrderEventPublisher`): Đã log khi gửi xong (orderId, orderCode, topic). Có thể log **trước** khi send (topic, key, payload summary) và trong callback log **partition, offset** từ `SendResult.getRecordMetadata()`.
  - **Consumer** (`OrderCreatedKafkaListener`): Đổi tham số từ `OrderCreatedPayload payload` sang `ConsumerRecord<String, OrderCreatedPayload> record` (hoặc thêm tham số `@Header(KafkaHeaders.RECEIVED_PARTITION) int partition`, `@Header(KafkaHeaders.OFFSET) long offset`) để log **topic, partition, offset, key** và payload summary (orderId, orderCode, totalAmount…). Code mẫu trong phần dưới.

#### Công dụng / Lợi ích

- **Topic**: Chủ động tạo topic đúng partition/replication, tránh dùng cấu hình mặc định của broker.
- **Consumer / offset**: Biết group nào đang chạy, lag bao nhiêu → phát hiện consumer chậm hoặc chết; reset offset khi cần replay.
- **Log message**: Trace được “message X đã gửi / đã nhận ở partition Y offset Z” → debug, audit, support.

#### Nhược điểm / Lưu ý

- Reset offset sai (vd: reset về latest khi đang có message chưa xử lý) → mất message. Luôn kiểm tra và dừng consumer khi reset.
- Log full payload nhiều → tốn I/O, log quá lớn; có thể lộ dữ liệu. Nên log summary (id, code, amount…) và dùng DEBUG cho full payload khi cần.

### 7.6. Tính toàn vẹn dữ liệu (giao dịch/thanh toán), cơ chế sâu và ứng dụng thực tế

*(Tài liệu chi tiết: **`kafka-data-integrity-and-deep-dive.md`**. Phần dưới tóm tắt ý chính.)*

#### Lý thuyết

- **Vấn đề với giao dịch/thanh toán trên Kafka**: (1) **Duplicate** – at-least-once khiến consumer có thể xử lý cùng message nhiều lần → trừ tiền/tồn kho hai lần. (2) **Mất event** – ghi DB xong chưa kịp gửi Kafka hoặc ngược lại. (3) **Thứ tự** – message cùng orderId phải xử lý đúng thứ tự. (4) **Eventual consistency** – dữ liệu giữa các service đồng bộ “sau một lúc”.
- **Các giải pháp đảm bảo tính toàn vẹn**:
  - **Idempotent consumer**: Mỗi message có key (eventId/orderId); consumer lưu “đã xử lý” (bảng processed_events). Lần 2 nhận cùng key → bỏ qua. **Bắt buộc** khi dùng at-least-once cho giao dịch.
  - **Transactional Outbox**: Ghi nghiệp vụ + bản ghi outbox (event payload) trong **cùng transaction DB**. Process riêng đọc outbox, gửi lên Kafka, rồi đánh dấu đã gửi. Đảm bảo “DB commit” ↔ “event sẽ được gửi”, không mất event.
  - **Saga**: Giao dịch phân tán qua nhiều service; mỗi bước là local transaction + event; khi lỗi dùng **compensating event** (hoàn tiền, hoàn tồn kho) thay vì 2PC.
  - **Exactly-once Kafka**: Idempotent producer (broker loại duplicate retry); transactional producer (commitTransaction); consumer read_committed. Có thể kết hợp KafkaTransactionManager để commit offset trong transaction.
  - **Thứ tự**: Gửi message với **partition key = business key** (orderId) → cùng key vào cùng partition → consumer xử lý đúng thứ tự trong partition.
- **Cơ chế vận hành sâu**: (1) **Lưu trữ**: partition = log append-only, segment, retention. (2) **Replication**: leader/follower, ISR; **acks=all** đợi toàn bộ ISR. (3) **Producer**: retry → duplicate nếu không idempotent; batching, compression. (4) **Consumer**: auto vs manual commit; rebalance khi join/leave group. (5) **Transaction**: begin/commit transaction; read_committed chỉ đọc message đã commit.

#### Cách ứng dụng trong PetBoby (và mở rộng)

- **Hiện tại**: OrderCreatedEvent dùng **key = orderId** → thứ tự theo order đã đảm bảo trong partition. Chưa có outbox; event gửi sau khi save Order (risk: save xong, crash trước khi gửi → mất event).
- **Gợi ý khi thêm Payment / giao dịch nhạy cảm**:
  - **Producer (Order)**: Thêm **eventId** (UUID) vào OrderCreatedEvent; bật **enable.idempotence=true**, **acks=all** cho producer. Cân nhắc **Transactional Outbox**: ghi Order + outbox row trong 1 transaction; job đọc outbox gửi Kafka.
  - **Consumer (Payment/Inventory)**: Bảng **processed_events(event_id hoặc order_id, processed_at)**; trước khi trừ tiền/tồn kho check đã xử lý chưa; xử lý xong insert processed_events + **manual commit offset**. Có thể dùng **DLQ** topic cho message lỗi sau N retry.
- **Scale & vận hành**: replication.factor >= 2; monitor consumer lag; partition >= số instance consumer; schema version trong event (hoặc schema registry) khi nhiều service.

#### Công dụng / Lợi ích

- **Idempotency**: Tránh trừ tiền/tồn kho hai lần; an toàn với at-least-once và rebalance.
- **Outbox**: Đảm bảo không mất event khi “ghi DB + gửi Kafka”; chuẩn production.
- **Saga**: Cho phép giao dịch trải nhiều service mà không 2PC; scale được.
- **Hiểu cơ chế sâu**: Tối ưu acks, partition, batch; debug lag, duplicate; chọn đúng commit strategy.

#### Nhược điểm / Lưu ý

- Outbox thêm bảng + process; Saga thiết kế phức tạp, có giai đoạn inconsistent tạm thời. Exactly-once Kafka cấu hình và vận hành nặng hơn.
- Idempotency cần lưu trữ key (DB); TTL nếu dùng cache phải đủ dài (retention Kafka). Partition key phải nhất quán (cùng orderId → cùng partition).

---

## 8. Hướng dùng file này

- **Cấu trúc mỗi mục** (xem bảng ở đầu file): **Lý thuyết** → **Cách ứng dụng** → **Công dụng/Lợi ích** → **Nhược điểm/Lưu ý**. Đọc đủ bốn phần để vừa hiểu khái niệm, vừa biết dùng ở đâu và tránh bẫy.
- Khi bạn học/ôn lại một kiến thức (ví dụ: **Kafka**, **RestTemplate**, **3-level cache**, **self-invocation AOP**):
  - Tìm mục tương ứng trong file này.
  - Đọc **Lý thuyết** để nắm “là gì, tại sao”; **Cách ứng dụng** để biết trong PetBoby nó nằm ở class/config nào; **Công dụng/Lợi ích** và **Nhược điểm** để biết khi nào dùng, khi nào cẩn thận.
  - Mở code được dẫn chiếu để **xem lại ví dụ sống trong `PetBoby`**.
  - Đọc thêm **`PetBoby-backend-knowledge-senior.md`** cho cùng topic để nắm trade-off, rủi ro production, và cách review.
- Khi thêm kiến thức mới (Redis, Liquibase, Security,...):
  - Viết mục mới theo đủ 4 phần: Lý thuyết, Cách ứng dụng, Công dụng/Lợi ích, Nhược điểm.
  - Bổ sung mục tương ứng trong **`PetBoby-backend-knowledge-senior.md`** khi có trade-off/production/review.
  - Áp dụng vào luồng nghiệp vụ thật (nếu hợp lý); nếu nhạy cảm thì demo riêng với comment “demo only, không vào luồng chính”.



