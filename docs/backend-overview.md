# Backend Overview – Kiến trúc & Endpoint `PetBoby`

Tài liệu này mô tả kiến trúc backend microservices của `PetBoby` và danh sách endpoint đã có, phục vụ **Phase 2 – Tuần 1: Microservices 101** và tham chiếu khi học Gateway / service-to-service.

---

## 1. Kiến trúc tổng quan

```
                    ┌─────────────────┐
                    │   Client (FE)    │
                    └────────┬────────┘
                             │ HTTP
                             ▼
                    ┌─────────────────┐
                    │ gateway-service │  port 8080
                    │ (Spring Cloud   │
                    │   Gateway)      │
                    └────────┬────────┘
                             │
         ┌────────────────────┼────────────────────┬───────────────────┐
         │                    │                    │                   │
         ▼                    ▼                    ▼                   ▼
  /api/auth/**        /api/users/**        /api/products/**    /api/orders/**
  auth (8084)         user (8081)          product (8082)      order (8083)
```

- **Monolith vs Microservices (PetBoby)**  
  PetBoby đã tách thành nhiều service độc lập (user, product, order, gateway). Mỗi service có DB riêng (hoặc schema riêng), chạy process riêng. Client chỉ gọi một cửa (gateway), gateway định tuyến theo path.

- **Bounded context (ranh giới nghiệp vụ)**  
  - **user**: tài khoản, role, profile, customer/employee, auth.  
  - **product**: category, product, topping, menu.  
  - **order**: đơn hàng, order item, topping trong đơn, thanh toán, trạng thái đơn.

- **Giao tiếp**  
  - **Đồng bộ (synchronous)**: client ↔ gateway ↔ service qua HTTP/REST; order gọi product (RestTemplate) để lấy giá/sản phẩm.  
  - **Bất đồng bộ (asynchronous)**: Kafka topic `order.created` – order publish `OrderCreated`, product service consumer log (Tuần 4).

---

## 2. Gateway routing (hiện tại)

File: `gateway-service/src/main/resources/application.yml`

| Path                  | Chuyển đến     | URI (backend)        |
|-----------------------|----------------|----------------------|
| `/api/auth/**`        | auth-service   | http://localhost:8084 |
| `/api/users/**`       | user-service   | http://localhost:8081 |
| `/api/products/**`    | product-service| http://localhost:8082 |
| `/api/orders/**`      | order-service  | http://localhost:8083 |
| `/api/demo/orders/**` | order (demo)   | http://localhost:8083 |

**Filter (Tuần 2 – API Gateway)**: Route `order-service` và `order-demo` dùng filter `AddRequestHeader=X-Gateway-Route, ...` để backend nhận biết request đi qua gateway.

**Ví dụ flow**:  
Client gọi `GET http://localhost:8080/api/products` → Gateway nhận, match `Path=/api/products/**` → forward đến `http://localhost:8082/api/products` (product service trả lời).

---

## 3. Các service & trách nhiệm

| Service          | Port (gợi ý) | Trách nhiệm chính |
|------------------|---------------|--------------------|
| gateway-service  | 8080          | Entry point, routing theo path, cross-cutting concerns (sau này: auth filter, logging). |
| auth             | 8084          | Login, phát hành JWT, điểm đầu cho xác thực & phân quyền. |
| user             | 8081          | User, profile, customer/employee. |
| product          | 8082          | Category, Product, Topping, menu API. |
| order            | (vd 8083)     | Order, OrderItem, OrderItemTopping; báo cáo theo branch; demo N+1. |

---

## 4. Endpoint đã có (qua gateway: base URL `http://localhost:8080`)

### 4.1. Product (prefix `/api/products`)

- `GET /api/products` – Danh sách product (query: `status`, `categoryId` tùy chọn).
- `GET /api/products/bulk?ids=1,2,3` – Bulk theo id (SELECT IN).
- `GET /api/products/{id}` – Chi tiết 1 product.
- `GET /api/products/{id}/toppings` – Topping của product (từ JSON field).
- `POST /api/products` – Tạo product (body: ProductCreateRequest).
- `PUT /api/products/{id}` – Cập nhật product (body: ProductUpdateRequest).
- `DELETE /api/products/{id}?deletedBy=...` – Soft delete (204).

### 4.2. Demo (product module, gọi trực tiếp product service 8082 hoặc qua gateway nếu route trùng)

- `GET /api/demo/scopes` – Demo bean scope.
- `POST /api/demo/jpa`, `POST /api/demo/jpa/{id}` – Demo JPA persistence context.

### 4.3. Order (qua gateway: `/api/orders/**` hoặc `/api/demo/orders/**` → 8083)

- **Demo N+1** (path `/api/demo/orders/...`):
  - `GET /api/demo/orders/nplus1/naive?branchCode=...&from=...&to=...` – Demo N+1 (naive).
  - `GET /api/demo/orders/nplus1/optimized?branchCode=...&from=...&to=...` – Demo N+1 (fetch join).
- **API nghiệp vụ order (Tuần 3 – Order gọi Product service)**:
  - `POST /api/orders` – Tạo đơn (body: branchCode, customerId?, paymentMethod, items: [{ productId, quantity }]). Order service gọi Product service (GET /api/products/bulk) để validate và lấy giá/tên, rồi lưu Order + OrderItems.
  - `GET /api/orders` – Danh sách đơn.
  - `GET /api/orders/{id}` – Chi tiết đơn.

### 4.4. Auth

- `POST /api/auth/login` – Nhận `LoginRequest { username, password }`, trả về `LoginResponse { accessToken, tokenType, expiresInSeconds }` với JWT (demo: user admin/admin).

### 4.5. User

- API user (CRUD, profile) sẽ bổ sung theo domain knowledge.

---

## 5. Sequence đơn giản: “User đặt hàng” (target sau khi có order API qua gateway)

1. Client gửi `POST /api/orders` (qua gateway) với payload (customerId, items, branchCode...).
2. Gateway forward đến order service.
3. Order service:
   - Gọi product service qua `ProductClient` (RestTemplate: GET /api/products/bulk?ids=...) để lấy giá / validate productId (Tuần 3).
   - Lưu Order + OrderItem + OrderItemTopping.
   - Publish event `OrderCreated` lên Kafka topic `order.created` (Tuần 4). Product service consumer log event.
4. Order service trả response về client (qua gateway).

---

File này nên được cập nhật khi thêm endpoint mới hoặc thay đổi route/port.
