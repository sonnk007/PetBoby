# RESILIENCE PATTERNS - Phase 4 Learning Guide

## 📚 Table of Contents
1. [Overview](#overview)
2. [Lý Thuyết Chi Tiết](#lý-thuyết-chi-tiết)
3. [Pattern 1: Retry](#pattern-1-retry)
4. [Pattern 2: Circuit Breaker](#pattern-2-circuit-breaker)
5. [Pattern 3: Timeout](#pattern-3-timeout)
6. [Pattern 4: Rate Limiter](#pattern-4-rate-limiter)
7. [Pattern 5: Bulkhead](#pattern-5-bulkhead)
8. [Pattern 6: Combined](#pattern-6-combined)
9. [Production Best Practices](#production-best-practices)
10. [Testing Guide](#testing-guide)

---

## Overview

**Resilience Patterns** là cách để code chạy ổn định khi hệ thống có vấn đề (mạng yếu, service down, tải cao).

**Tại sao cần?**
- Production = không lý tưởng. API bên ngoài có thể chậm, DB có thể timeout, network có thể drop packet.
- Không có resilience → "cascade failure" → toàn bộ hệ thống sập.
- Ví dụ: order service chờ product service → product service chết lâu → order service cạn thread → user service chờ order service → cả hệ thống đông.

**5 Pattern Chính:**
| Pattern | Công Dụng | Khi Nào Dùng |
|---------|----------|-----------|
| **Retry** | Thử lại tự động | Lỗi tạm thời (network hiccup, DB bị tải spike) |
| **Circuit Breaker** | Mở circuit khi liên tục gặp lỗi | Service gọi đã chết, tránh lãi thêm request |
| **Timeout** | Đặt thời gian chờ tối đa | Bảo vệ khỏi hanging request, cạn thread pool |
| **Rate Limiter** | Giới hạn request/giây | API public, tránh abuse hoặc quá tải |
| **Bulkhead** | Chia resource thành từng phần riêng | Ngăn một endpoint chậm chặn endpoint khác |

**Framework:** `Resilience4j` (successor của Hystrix, nhẹ hơn, flexible hơn)

---

## Lý Thuyết Chi Tiết

### Thông Tin Chung

**Resilience4j là gì?**
- Java library cung cấp các decorator/aspect cho resilience.
- Annotation-based: `@Retry`, `@CircuitBreaker`, `@TimeLimiter`, `@RateLimiter`, `@Bulkhead`.
- Spring Boot auto-config: load config từ `application.yml`.

**Cấu hình:**
```yaml
resilience4j:
  retry:
    instances:
      productService:
        max-attempts: 3
        wait-duration: 1000
        # ...
  circuitbreaker:
    instances:
      productService:
        failure-rate-threshold: 50
        # ...
```

**Fallback Method:**
- Một số pattern (CircuitBreaker, RateLimiter, Bulkhead) hỗ trợ `fallbackMethod`.
- Khi pattern kích hoạt → fallback được gọi thay vì throw exception.
- Signature: phải match return type + thêm parameter `Throwable` cuối cùng.

```java
@CircuitBreaker(name = "prod", fallbackMethod = "fallback")
public String call() { ... }

public String fallback(Throwable e) {
    return "fallback response";
}
```

---

## Pattern 1: Retry

### Khái Niệm

Thử lại tự động khi request fail. Hữu ích cho lỗi tạm thời (network flaky, timeout tạm thời).

### Cơ Chế

```
Request 1 → Fail (IOException)
  ↓
Wait 1s (backoff)
  ↓
Request 2 → Fail (TimeoutException)
  ↓
Wait 2s (exponential: 1s * 2.0)
  ↓
Request 3 → Success ✓
```

### Config trong PetBoby

File: `order/src/main/resources/application.yml`

```yaml
resilience4j:
  retry:
    instances:
      productService:
        max-attempts: 3
        wait-duration: 1000  # start wait time in ms
        multiplier: 2.0      # exponential backoff
        retry-exceptions:    # retry on these
          - java.net.ConnectException
          - java.io.IOException
          - org.springframework.web.client.ResourceAccessException
        ignore-exceptions:   # never retry on these
          - org.springframework.web.client.HttpClientErrorException  # 4xx
```

### Code Demo

File: `order/src/main/java/com/sonnk/order/demo/ResiliencePatternDemoService.java`

```java
@Retry(name = "productService")
public String getProductWithRetry() {
    log.info("Attempting to call Product service...");
    try {
        String response = restTemplate.getForObject(
            "http://localhost:8082/api/products/1",
            String.class
        );
        return response;
    } catch (RestClientException e) {
        log.error("Product service call failed: {}", e.getMessage());
        throw e;
    }
}
```

### Thế Nào Là Chốt Hạ?

Log sẽ show:
```
[INFO] Attempting to call Product service (with retry)...
[ERROR] Product service call failed: Connection refused
[INFO] Retry 'productService' - Attempt 1 at ConnectException
[WARN] Retry 'productService' - Wait 1000ms before retry...
[INFO] Attempting to call Product service (with retry)...
[ERROR] Product service call failed: Connection refused
[INFO] Retry 'productService' - Attempt 2 at ConnectException
[WARN] Retry 'productService' - Wait 2000ms before retry...  # exponential: 1s * 2
...
```

### Khi Nào Dùng?

✅ **Dùng:**
- Network timeout tạm thời → retry lại → thường thành công.
- DB bị tải spike tạm thời → request timeout → retry sau 1s → đã hết spike.

❌ **Không dùng:**
- POST request không idempotent (mỗi submit, tạo 1 bản ghi mới) → retry = tạo duplicate.
  - **Fix:** Thêm request ID, server kiểm tra idempotency.
- Lỗi 404 (resource không tồn tại) → retry vô ích.

### Senior Tip: Retry + Database Idempotency

Scenario: POST order, nếu timeout → retry → tạo 2 order?

**Solution:**
1. Client gửi `X-Idempotency-Key: {UUID}` trong header.
2. Server check: nếu idempotency key đã tồn tại → return cached result thay vì create mới.

```java
CREATE UNIQUE INDEX idx_order_idempotency_key ON order(idempotency_key);

@PostMapping
public ResponseEntity<OrderResponse> createOrder(
    @RequestBody OrderRequest req,
    @RequestHeader("X-Idempotency-Key") String idempotencyKey
) {
    // Check if already exists
    Optional<Order> existing = orderRepository.findByIdempotencyKey(idempotencyKey);
    if (existing.isPresent()) {
        return ResponseEntity.ok(toResponse(existing.get())); // Return cached
    }
    
    // Create new
    Order order = new Order(req);
    order.setIdempotencyKey(idempotencyKey);
    orderRepository.save(order);
    return ResponseEntity.status(201).body(toResponse(order));
}
```

---

## Pattern 2: Circuit Breaker

### Khái Niệm

Giống công tắc điện: nếu liên tục gặp lỗi → mở công tắc → từ chối request ngay (fail-fast) thay vì cố gắng.

### Cơ Chế

**3 trạng thái:**

```
CLOSED (bình thường)
  ↓ (50% lỗi trong 10 call gần nhất)
OPEN (mở công tắc, từ chối request)
  ↓ (chờ 5s)
HALF_OPEN (thử lại, test 3 call)
  ↓ (tất cả ok → CLOSED)
  ↓ (có lỗi → OPEN lại)
```

### Diagram

```
CLOSED [✓request → service]
   ↑                    ↓
   │            [lỗi accumulate:
   │             50% in 10 calls]
   │                    ↓
   └─────→ OPEN [✗reject immediately]
           (wait 5s)
              ↓
        HALF_OPEN [test 3 calls]
        ↓success        ↓fail
      CLOSED           OPEN
```

### Config trong PetBoby

```yaml
resilience4j:
  circuitbreaker:
    instances:
      productService:
        failure-rate-threshold: 50           # % failure
        minimum-number-of-calls: 10          # before evaluate
        automatic-transition-from-open-to-half-open-enabled: true
        wait-duration-in-open-state: 5000    # ms
        permitted-number-of-calls-in-half-open-state: 3
        sliding-window-type: COUNT_BASED
        sliding-window-size: 10              # last 10 calls
```

### Code Demo

```java
@CircuitBreaker(
    name = "productService",
    fallbackMethod = "fallbackGetProductCircuitBreaker"
)
public String getProductWithCircuitBreaker() {
    log.info("Calling Product service...");
    String response = restTemplate.getForObject(
        "http://localhost:8082/api/products/1",
        String.class
    );
    return response;
}

public String fallbackGetProductCircuitBreaker(Throwable ex) {
    log.warn("Circuit breaker active! Using fallback. Cause: {}", ex.getMessage());
    return """
        {"id": 0, "name": "FALLBACK", "status": "SERVICE_UNAVAILABLE", 
         "message": "Product service is currently unavailable"}
        """;
}
```

### Thế Nào Là Chốt Hạ?

Log sẽ show state transitions:

```
[WARN] Circuit Breaker 'productService' transitioned from CLOSED to OPEN
[WARN] Service is currently unavailable. Rejected request immediately without calling service.
[WARN] Circuit Breaker 'productService' transitioned from OPEN to HALF_OPEN
[INFO] Testing 3 calls in HALF_OPEN state...
[INFO] Call 1 succeeded
[INFO] All 3 calls succeeded → Circuit Breaker 'productService' transitioned from HALF_OPEN to CLOSED
```

### Khi Nào Dùng?

✅ **Dùng:**
- Gọi downstream service (product, user, payment).
- Tránh "cascade failure": downstream die → upstream chết kéo theo.

❌ **Không dùng:**
- Không cần nếu call idempotent + retry đủ rồi.

### Senior Tip: Circuit Breaker vs Retry

**Kết hợp đúng:**
```java
@Retry(name = "productService")  // Thử lại 3 lần
@CircuitBreaker(
    name = "productService",      // Nếu cả 3 lần đều lỗi → circuit breaker count
    fallbackMethod = "fallback"
)
public String call() { ... }
```

**Flow:**
- Request 1 timeout → Retry kích hoạt → thử lại 3 lần.
- Circuit breaker "nhìn thấy" 3 lỗi → **3 lỗi / 10 calls = 30% < 50% threshold → còn CLOSED**.
- Request 2-8 như trên → tích lũy lỗi.
- Request 10: tổng 50% lỗi → circuit mở.
- Request 11: circuit OPEN → **từ chối ngay, không retry**.

---

## Pattern 3: Timeout

### Khái Niệm

Đặt thời gian chờ tối đa. Quá thời gian → fail ngay (dù request vẫn đang chạy backend).

### Cơ Chế

```
Request start
  ↓
[chờ 5s]
  ↓
TimeLimiter timeout → throw TimeoutException
  ↓
Response ngay (không chờ backend response lâu hơn)
```

### Config

```yaml
resilience4j:
  timelimiter:
    instances:
      productService:
        timeout-duration: 5000  # ms
```

### Code Demo

```java
@TimeLimiter(name = "productService")
public CompletableFuture<String> getProductWithTimeout() {
    log.info("Calling Product service (with timeout)...");
    return CompletableFuture.supplyAsync(() -> {
        String response = restTemplate.getForObject(
            "http://localhost:8082/api/products/1",
            String.class
        );
        return response;
    });
}
```

**Note:** `@TimeLimiter` yêu cầu return `CompletableFuture` hoặc `Supplier` để có thể interrupt.

### Thế Nào Là Chốt Hạ?

```
[INFO] Calling Product service (with timeout)...
[WARN] TimeLimiter: Request takes too long (> 5000ms)
java.util.concurrent.TimeoutException: TimeLimiter 'productService' recorded a timeout
```

### Khi Nào Dùng?

✅ **Dùng:**
- HTTP call bên ngoài (có thể hang).
- DB query chậm (missing index, N+1).
- Tránh cạn thread pool.

❌ **Không dùng:**
- Internal method call (không cần timeout, chỉ cần xem performance).

### Senior Tip: Timeout Cascade

**Lỗi phổ biến:**

```
Request flow: Gateway (1min) → Order (30s) → Product (20s)

Nếu Product trả về sau 25s:
- Gateway timeout 1min → ok, vẫn chờ.
- Order timeout 30s → ok, vẫn chờ.
- Product response 25s (< 20s timeout??) → BUG!
```

**Fix:** Timeout ở lớp cha phải > lớp con.

```
Gateway timeout: 60s
Order timeout: 30s (= Product call + local process time)
Product call timeout: 20s (actual call)
```

---

## Pattern 4: Rate Limiter

### Khái Niệm

Giới hạn số request xử lý trong thời gian (vd: 10 req/s). Protect resource khỏi bị overwhelm.

### Cơ Chế

```
Token Bucket algorithm:
- Bucket có capacity 10 token.
- Mỗi giây fill thêm 10 token.
- Mỗi request cần 1 token.
- Hết token → reject request.

Timeline:
Time 0: 10 token
Request 1-10: ok, token 10→0
Request 11: reject (0 token)
Time 1: refill 10 token (sáng hôm sau)
Request 12: ok, token 10→9
...
```

### Config

```yaml
resilience4j:
  ratelimiter:
    instances:
      productServiceBulk:
        limit-for-period: 10        # max requests
        limit-refresh-period: 1s    # per time window
        timeout-duration: 5s        # chờ token tối đa
```

### Code Demo

```java
@RateLimiter(
    name = "productServiceBulk",
    fallbackMethod = "fallbackRateLimit"
)
public String bulkGetProducts() {
    return "Retrieving 100 products...";
}

public String fallbackRateLimit(Throwable ex) {
    return "Rate limit exceeded. Please retry after some time.";
}
```

### Thế Nào Là Chốt Hạ?

```
Request 1-10 trong 1s: ok
Request 11: [WARN] Rate limit exceeded, RequestNotPermitted thrown
Response: HTTP 429 Too Many Requests
Fallback: "Rate limit exceeded. Please retry after some time."
```

### Khi Nào Dùng?

✅ **Dùng:**
- API public (ngăn abuse).
- Bulk operation (bulk upload, export).
- Protect resource đặc biệt (report generation, AI inference).

❌ **Không dùng:**
- Internal API (giữa service).

### Senior Tip: Local vs Global Rate Limit

**Local rate limiter (current):**
- Per instance (nếu 3 server, mỗi cái 10 req/s = total 30).

**Global rate limiter (distributed):**
- Dùng Redis:

```java
// Redis-backed rate limiter (more complex setup)
RateLimiter rateLimiter = RateLimiterRegistry.ofRedisBackedRateLimiter()
    .build()
    .rateLimiter("productApi", RedisRateLimiterConfig.custom()
        .limitForPeriod(100)
        .limitRefreshDuration(Duration.ofSeconds(1))
        .build());
```

---

## Pattern 5: Bulkhead

### Khái Niệm

Chia resource thành từng phần riêng (isolate) → failure ở một phần không ảnh hưởng phần khác.

Hình dung: tàu có bulkhead (vách ngăn) → hỏng ngăn trước không ảnh hưởng ngăn sau.

### Cơ Chế

```
Thread pool size: 200 (mặc định của Tomcat)

Kiến trúc A (NO Bulkhead):
┌─────────────────────────────┐
│ /api/analytics/ (chạy 30s)  │  hoàn toàn cho analytics
│ sử dụng tất cả 200 thread   │
│                             │
│ /api/orders/123 (lấy detail)│  phải chờ thread trống
│ không có thread available   │
└─────────────────────────────┘

Kiến trúc B (WITH Bulkhead):
┌──────────────────┬──────────────────────┐
│ Analytics bulkhead │ Orders bulkhead     │
│ max 10 threads     │ max 190 threads     │
│                  │                      │
│ Analytics có thể  │ Orders có enough    │
│ 200/(10+190)=10  │ thread, response fast│
└──────────────────┴──────────────────────┘
```

### Config

```yaml
resilience4j:
  bulkhead:
    instances:
      productService:
        max-concurrent-calls: 10     # tối đa 10 call cùng lúc
        max-wait-duration: 5s        # chờ slot trống tối đa 5s
```

### Code Demo

```java
@Bulkhead(
    name = "productService",
    fallbackMethod = "fallbackBulkhead"
)
public String heavyAnalytics() {
    log.info("Running heavy analytics...");
    Thread.sleep(2000); // simulate heavy work
    return "Analytics completed";
}

public String fallbackBulkhead(Throwable ex) {
    return "Service too busy (bulkhead full), please retry later";
}
```

### Thế Nào Là Chốt Hạ?

```
11 concurrent request gọi /api/demo/resilience/bulkhead:
Request 1-10: ok, acquiring bulkhead slot
Request 11: [WARN] Bulkhead is at capacity, rejecting request
           BulkheadFullException thrown
           Fallback: "Service too busy (bulkhead full)"
```

### Khi Nào Dùng?

✅ **Dùng:**
- Nhiều endpoint cạnh tranh resource.
- Endpoint slow (analytics, export, heavy processing).

❌ **Không dùng:**
- Endpoint nhanh (không cần isolate).

### Senior Tip: Bulkhead vs ThreadPool

**Bulkhead (Isolation):**
- Cách tiếp cận 1: **shared thread pool** với bulkhead instance.
- Tất cả đều dùng chung thread pool, nhưng bulkhead giới hạn.

**ThreadPool (Separate):**
- Cách tiếp cận 2: **dedicated thread pool** cho mỗi endpoint.
- Analytics có thread pool riêng, Orders có thread pool riêng.
- Phức tạp hơn nhưng isolate tốt hơn.

**Resilience4j default:** shared thread pool + bulkhead config.

---

## Pattern 6: Combined

### Khái Niệm

Kết hợp nhiều pattern để tạo end-to-end resilience.

### Stack Order & Execution Flow

#### Why Stack Order Matters?

Thứ tự execution của các pattern **rất quan trọng**. Nếu sai order → hệ thống lại bị lỗi tương tự như không có pattern.

**Rule cơ bản:** Pattern ngoài cùng (trên) phải **cheap** (nhanh, ít resource), pattern trong cùng (dưới) mới **expensive** (gọi service).

```
OUTER (cheap checks) ─────→ INNER (expensive operations)

RateLimiter (quick boolean check)
    ↓
Bulkhead (thread pool check)
    ↓
TimeLimiter (timeout setup)
    ↓
CircuitBreaker (state machine check)
    ↓
Retry (potential retry loop)
    ↓
Actual method call (HTTP I/O - EXPENSIVE!)
```

---

### Giải Thích Chi Tiết - Stack Order & Công Dụng

#### **Layer 1: RateLimiter (Outermost Gate)**

**Mục đích:** 
- Chặn abuse từ một lúc (giảm pressure trước khi vào hệ thống).
- Không cho phép > 10 request/giây → reject ngay với HTTP 429.

**Công dụng:**
- Bảo vệ backend khỏi DDoS / spike traffic.
- Nếu client spam request → RateLimiter reject trước khi đi vào, tiết kiệm CPU/memory.
- Tính toán: 10 req/s × 5000ms timeout = tối đa 50 concurrent request→ thread pool không cơm sẩm.

**Khi nào trigger:**
```
Request rate > 10 req/s
→ RateLimiter reject (throw RequestNotPermitted)
→ HTTP 429 Too Many Requests
→ Fallback: "Rate limit exceeded"
```

**Lợi ích:**
- ✅ Giảm load trước khi vào bulkhead/retry.
- ✅ Nhanh (chỉ token bucket check, O(1) complexity).
- ✅ Công bằng (fairness): request thứ 11 từ chối, không xắp hàng vô tận.

---

#### **Layer 2: Bulkhead (Resource Isolation)**

**Mục đích:**
- Giới hạn concurrent request cực cùng lúc (tối đa 10).
- Nếu đã có 10 request đang chạy → request thứ 11 chờ 5s hoặc reject.
- Ngăn endpoint này "cản" endpoint khác (isolate).

**Công dụng:**
- Bảo vệ thread pool khỏi bị cạn.
- Scenario: `/api/analytics` (chạy 30s) + `/api/orders/1` (lấy 50ms).
  - **Không bulkhead:** 200 analytics request × 30s = 200 thread cạn → orders phải chờ.
  - **Có bulkhead:** analytics tối đa 10 thread → 190 thread cho orders → orders response fast.

**Khi nào trigger:**
```
11 concurrent request cùng lúc
→ Request 1-10: ok (acquire bulkhead slot)
→ Request 11: chờ 5s (wait for slot)
  → sau 5s vẫn không có slot → reject (BulkheadFullException)
  → Fallback: "Service too busy"
```

**Lợi ích:**
- ✅ Isolate heavy endpoint.
- ✅ Tránh thundering herd (spike concurrent).
- ✅ Dễ debug: biết chính xác max concurrent là bao nhiêu.

---

#### **Layer 3: TimeLimiter (Absolute Timeout)**

**Mục đích:**
- Đặt timeout tuyệt đối cho **toàn bộ operation** (included retry).
- Nếu operation > 5s → timeout ngay, không chờ thêm.

**Công dụng:**
- Bảo vệ thread khỏi **bị block vô hạn**.
- Scenario: network rất chậm, retry lần 1 chờ 1s, lần 2 chờ 2s, lần 3 chờ 4s = 7s tổng.
  - **Không timeout:** request chết mất 7s (thread cạn).
  - **Có timeout (5s):** sau 5s fail → thread tự do ngay.

**Khi nào trigger:**
```
Operation (retry loop) > 5 seconds
→ TimeLimiter interrupt
→ TimeoutException
→ Fail-fast (không chờ retry lần 3 kết thúc)
```

**Lợi ích:**
- ✅ Tránh hanging thread (deadlock).
- ✅ Garantee response time (SLA: 95% requests < 5s).
- ✅ Đặc biệt quan trọng khi retry + exponential backoff kết hợp.

**⚠️ Timeout Cascade Rule:**
```
Client timeout: 60s
↓ Gateway timeout: phải < 60s (vd: 55s)
↓ Order service timeout: phải < 55s (vd: 30s)
  ↓ Product call timeout: phải < 30s (vd: 5s via TimeLimiter)
```

---

#### **Layer 4: CircuitBreaker (State Machine)**

**Mục đích:**
- Nhận diện service "chết" (OPEN state).
- Nếu 50% fail trong 10 call gần nhất → mở circuit → từ chối request.
- Tránh "cascade failure": order service chết → user service chờ mãi → gateway timeout.

**Công dụng:**
- **Fail-fast pattern:** Không cố gắng gọi dead service.
- **Auto-recovery:** Sau 5s auto chuyển HALF_OPEN, test 3 call, nếu ok → CLOSED.

**3 Trạng thái:**

```
CLOSED          HALF_OPEN         OPEN
(Normal)        (Testing)         (Service Dead)

✓ Call pass → Count lỗi     ✓ Call ok → CLOSED
✗ 50% lỗi → OPEN pending    ✗ Call fail → OPEN lại


Timeline Example:
─────────────────────────────────────────────────
Time 0-4s:   CLOSED, accumulate failures (5 lỗi/10 calls = 50%)
Time 5s:     Transition CLOSED → OPEN
Time 5-10s:  OPEN, reject all request (no-op fallback)
Time 10s:    Auto-transition OPEN → HALF_OPEN (wait-duration=5s)
Time 10-13s: Test 3 call in HALF_OPEN
Time 13s:    If all 3 ok → HALF_OPEN → CLOSED
             If any fail → HALF_OPEN → OPEN (restart 5s wait)
```

**Khi nào trigger:**
```
Failure rate (50%) in recent 10 calls → OPEN
Từ chối request ngay (HTTP 503 / fallback)
```

**Lợi ích:**
- ✅ Tránh cascade failure giữa service.
- ✅ Auto-recovery.
- ✅ Fail-fast (không waste time retry khi dead).

---

#### **Layer 5: Retry (Resilience to Transient Failures)**

**Mục đích:**
- Thử lại khi lỗi tạm thời (network hiccup, DB tải spike).
- Exponential backoff: 1s → 2s → 4s (để resource khôi phục).

**Công dụng:**
- Recovery từ **transient failures** (lỗi tạm thời).
- Không phục vụ cho **permanent failures** (service down).

**Khi nào trigger:**
```
IOException / ConnectException / TimeoutException
→ Retry attempt 1 (wait 1s)
→ If fail again: Retry attempt 2 (wait 2s)
→ If fail again: Retry attempt 3 (wait 4s)
→ If all 3 fail: throw exception (circuit breaker count)
```

**Lợi ích:**
- ✅ Recover từ network glitch.
- ✅ Exponential backoff giúp DB/service khôi phục.
- ✅ Giảm false negative error.

**⚠️ Lưu ý Idempotency:**
- Chỉ retry **idempotent operation** (GET, DELETE).
- POST phải có `idempotency-key` để tránh duplicate.

---

#### **Layer 6: Actual Method Call (HTTP I/O)**

**Mục đích:**
- Gọi HTTP service thực tế.

**Công dụng:**
- Thực thi nghiệp vụ (get product, create order).

---

### Combined Stack - Flow Diagram Chi Tiết

```
┌─────────────────────────────────────────────────────────┐
│ Client Request arrives                                  │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
        ╔═════════════════════════════════════╗
        ║ 1️⃣  RateLimiter Check (10 req/s)   ║
        ║──────────────────────────────────────║
        ║ Bucket tokens enough?                ║
        ╚═════════════════════════════════════╝
               │                │
      (OK)     ▼               ▼  (FAIL: no token)
               │           HTTP 429
               │       Fallback Response
               │
        ╔═════════════════════════════════════╗
        ║ 2️⃣  Bulkhead Check (max 10 concurrent)║
        ║──────────────────────────────────────║
        ║ Thread slot available?              ║
        ║ - If no: wait 5s for slot           ║
        ║ - If timeout: reject                ║
        ╚═════════════════════════════════════╝
               │                │
      (OK)     ▼               ▼  (FAIL: no slot after 5s)
               │           BulkheadFullException
               │       Fallback Response
               │
        ╔═════════════════════════════════════╗
        ║ 3️⃣  TimeLimiter Start (5s timeout)  ║
        ║──────────────────────────────────────║
        ║ Start 5-second countdown             ║
        ╚═════════════════════════════════════╝
               │
               ▼
        ╔═════════════════════════════════════╗
        ║ 4️⃣  CircuitBreaker State Check      ║
        ║──────────────────────────────────────║
        ║ - CLOSED: continue                  ║
        ║ - HALF_OPEN: test this call         ║
        ║ - OPEN: reject immediately          ║
        ╚═════════════════════════════════════╝
         C/H/O │
           │   ├─OPEN──┐
           │   │       └─→ HTTP 503 / Fallback
           │   │
           ├─CLOSED/HALF_OPEN
           │
           ▼
        ╔═════════════════════════════════════╗
        ║ 5️⃣  Retry Loop (attempt ≤ 3)       ║
        ║──────────────────────────────────────║
        ║ Attempt 1: HTTP call (0ms wait)     ║
        ║ - Success? → Return result ✓         ║
        ║ - Fail? → exponential backoff        ║
        ║                                      ║
        ║ Attempt 2: HTTP call (1s wait)      ║
        ║ - Success? → Return result ✓         ║
        ║ - Fail? → exponential backoff        ║
        ║                                      ║
        ║ Attempt 3: HTTP call (2s wait)      ║
        ║ - Success? → Return result ✓         ║
        ║ - Fail? → count for circuit breaker  ║
        ║                                      ║
        ║ ⚠️  TOTAL TIME: 0 + 1 + 2 = 3s      ║
        ║     < 5s TimeLimiter? OK ✓           ║
        ╚═════════════════════════════════════╝
               │           │
      (OK)     ▼           ▼  (FAIL: all retries exhausted)
               │       Exception / HTTP 503
               ▼
        ╔═════════════════════════════════════╗
        ║ 6️⃣  Actual HTTP Call (Product API) ║
        ║──────────────────────────────────────║
        ║ GET http://localhost:8082/api/...   ║
        ║ RestTemplate timeout: 5s read wait  ║
        ╚═════════════════════════════════════╝
               │
               │ Response / Exception
               │
               ▼
        ╔═════════════════════════════════════╗
        ║ 7️⃣  TimeLimiter Cancel              ║
        ║──────────────────────────────────────║
        ║ Operation completed < 5s            ║
        ║ Cancel timeout                      ║
        ╚═════════════════════════════════════╝
               │
               ▼
        ┌─────────────────────────────────────┐
        │ Response to Client                  │
        │ - Success: entity data              │
        │ - Fail: error JSON + status code    │
        └─────────────────────────────────────┘
```

---

### Actual Scenario Examples

#### **Scenario 1: Normal Request (Happy Path)**

```
Request arrives
→ Rate Limiter: ok (2 req/s < 10)
→ Bulkhead: ok (3 concurrent < 10)
→ TimeLimiter: start 5s countdown
→ Circuit Breaker: CLOSED (service healthy)
→ Retry: attempt 1
  → HTTP call: 50ms → SUCCESS ✓
→ TimeLimiter: cancel (50ms < 5s)
→ Return product data to client
```

**Total time: ~50ms**

---

#### **Scenario 2: Network Glitch (Transient Failure)**

```
Request arrives
→ Rate Limiter: ok
→ Bulkhead: ok
→ TimeLimiter: start 5s countdown
→ Circuit Breaker: CLOSED
→ Retry: attempt 1
  → HTTP call: timeout after 2s (network slow)
  → IOException
→ Retry: wait 1s, then attempt 2
  → HTTP call: 100ms → SUCCESS ✓
→ TimeLimiter: cancel (1s + 2s + 100ms = ~3.1s < 5s)
→ Return product data
```

**Total time: ~3s (1 error + 1 retry)**
**Benefit:** Recovered từ network glitch automatically.

---

#### **Scenario 3: Product Service Dead (Permanent Failure)**

```
Request 1-5 arrives
→ Rate Limiter: ok × 5
→ Bulkhead: ok × 5
→ Circuit Breaker: CLOSED
→ Retry × 5:
  → Attempt 1-3: all fail (service unreachable)
  → 5 × 3 = 15 failures
→ CircuitBreaker observe: 15 failures / recent 10 calls = 150% (all fail)
  → CIRCUIT OPENS ❌

Request 6 arrives
→ Rate Limiter: ok
→ Bulkhead: ok
→ Circuit Breaker: OPEN ❌
  → Reject immediately (no HTTP call)
  → Fallback: "Product service unavailable"
  → HTTP 503 to client
→ TimeLimiter: cancel (< 1ms, just state check)
```

**Benefit:** 
- Request 1-5 failed (unavoidable, service dead).
- Request 6: rejected immediately (no waste of 3-attempt × 2s = 6s).
- Circuit Breaker auto-recovery: wait 5s → HALF_OPEN → test 3 calls → if ok → CLOSED.

---

#### **Scenario 4: Rate Limit Exceeded (Spike Traffic)**

```
Request 1-10 arrives (fast)
→ Rate Limiter: ok (within 10 req/s)

Request 11 arrives (in same 1-second window)
→ Rate Limiter: FAIL ❌ (exceeds 10 req/s)
  → HTTP 429 Too Many Requests
  → Fallback: "Rate limit exceeded, retry later"
  → Return immediately (< 1ms)

Request 12-20 arrives
→ Rate Limiter: all FAIL (still within 1-sec window)

Wait 1+ second (token bucket refill)
Request 21+ arrives
→ Rate Limiter: ok (new second, 10 token refill)
```

**Benefit:**
- Spike traffic contained at gate (rate limiter).
- Backend không bị overwhelm.
- Client know to retry later (HTTP 429 + message).

---

#### **Scenario 5: Bulkhead - Slow Analytics Endpoint**

```
10 requests to /api/analytics (slow, 30s each)
→ Rate Limiter: ok × 10
→ Bulkhead: acquire slot × 10
  → Executing in thread pool (10 threads occupied)

Request 11 to /api/orders/123 (fast, should be 50ms)
→ Rate Limiter: ok (within 10 req/s)
→ Bulkhead: FULL (all 10 slots occupied by analytics)
  → Wait up to 5s for slot
  → 5s timeout → BulkheadFullException
  → Fallback: "Service too busy"
  → HTTP 503

Without Bulkhead:
  Request 11 would queue in thread pool (Tomcat 200 threads)
  → 10 analytics + 190 normal request = 200 threads full
  → All new request timeout
  → Cascade failure ❌

With Bulkhead:
  Analytics use 10 dedicate slots
  → Normal request can use remaining slots
  → Orders still respond fast ✓
```

---

### Key Takeaways

| Layer | Purpose | Check Time | Action |
|-------|---------|-----------|--------|
| **RateLimiter** | Throttle incoming | ~0ms | Reject if > 10 req/s |
| **Bulkhead** | Isolate resource | ~0ms | Reject if > 10 concurrent |
| **TimeLimiter** | Timeout guarantee | ~0ms setup | Fail if > 5s total |
| **CircuitBreaker** | Detect dead service | ~0.1ms | Reject if OPEN |
| **Retry** | Recover transient | 1-4s wait | Exponential backoff |
| **HTTP Call** | Execute | variable | Actual I/O |

---

### Summary - Why This Order?

```
RateLimiter  → Cheap (boolean check)   ✓
Bulkhead     → Cheap (slot count)      ✓
TimeLimiter  → Setup (no I/O)          ✓
CircuitBreaker → Cheap (state read)    ✓
Retry        → Potential delay (wait)  ⚠️
HTTP Call    → Expensive (I/O)         ❌❌❌

= Cost increases as go deeper
= Stop early if possible (fail-fast philosophy)
```

### Code Demo

```java
@Retry(name = "productService")
@CircuitBreaker(
    name = "productService",
    fallbackMethod = "fallbackCombined"
)
@TimeLimiter(name = "productService")
@RateLimiter(name = "productServiceBulk")
public CompletableFuture<String> getProductCombined() {
    log.info("Calling with ALL patterns combined...");
    return CompletableFuture.supplyAsync(() -> {
        String response = restTemplate.getForObject(
            "http://localhost:8082/api/products/1",
            String.class
        );
        return response;
    });
}

public CompletableFuture<String> fallbackCombined(Throwable ex) {
    return CompletableFuture.completedFuture(
        "Fallback: Service temporarily unavailable"
    );
}
```

### Flow Diagram

```
Request arrives
    ↓
Rate Limit check (10/s)
    → reject? → HTTP 429
    ↓ ok
Bulkhead check (max 10 concurrent)
    → full & timeout 5s? → BulkheadFullException
    ↓ ok
TimeLimiter start (5s timeout)
    ↓
Circuit Breaker check
    → OPEN? → fallback or throw
    ↓ CLOSED/HALF_OPEN
Retry attempt 1
    → fail (IOException)? → wait 1s → Retry attempt 2
    → fail (TypeError)? → wait 2s → Retry attempt 3
    → ok? → TimeLimiter cancel timeout → return response
    ↓ all retries failed
TimeLimiter check (<5s?)
    → yes? → return error
    → no? → TimeLimiter timeout exception
```

---

## Production Best Practices

### 1. Configuration

**DO:**
- Config via `application.yml` → dễ thay đổi theo environment.
- Mỗi service dependency có riêng instance:
  ```yaml
  resilience4j:
    retry:
      instances:
        productService: { ... }
        userService: { ... }  # separate
    circuitbreaker:
      instances:
        productService: { ... }
        userService: { ... }  # separate
  ```

**DON'T:**
- Hardcode timeout/retry vào code.
- Global retry config cho tất cả → một service slow → toàn bộ bị retry.

### 2. Monitoring & Logging

**DO:**
- Log mỗi pattern event (Retry attempt, Circuit Breaker state change).
- Metrics: expose Micrometer metrics cho Prometheus.
- Alert: circuit breaker mở > 5 phút → alert.

**Code:**
```yaml
logging:
  level:
    io.github.resilience4j: DEBUG
    
# Micrometer integration (auto in Spring Boot)
management:
  endpoints:
    web:
      exposure:
        include: prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### 3. Testing

**DO:**
- Test từng pattern riêng (unit test).
- Test combined patterns (integration test với real/mock service).
- Test fallback methods.

**Example:**
```java
@Test
void testRetryRecovery() {
    // Given: service fail 2 times then success
    mockServer.stubFor(
        get("/api/products/1")
            .inScenario("retry test")
            .whenScenarioStateIs("Scenario.INITIAL")
            .willReturn(serverError())
            .willSetStateTo("attempt_1")
    ).stubFor(
        get("/api/products/1")
            .inScenario("retry test")
            .whenScenarioStateIs("attempt_1")
            .willReturn(serverError())
            .willSetStateTo("attempt_2")
    ).stubFor(
        get("/api/products/1")
            .inScenario("retry test")
            .whenScenarioStateIs("attempt_2")
            .willReturn(ok(json))
    );
    
    // When
    String result = demoService.getProductWithRetry();
    
    // Then
    assertNotNull(result);
}
```

### 4. Fallback Strategy

**DO:**
- Fallback có ý nghĩa: cache, default value, graceful degradation.
- Không fallback = let it fail (exception propagate hay không?).

**DON'T:**
- Fallback mà lại gọi downstream service khác (chain failure).

**Example (Good):**
```java
@CircuitBreaker(name = "prod", fallbackMethod = "fallback")
public String getProduct() { ... }

public String fallback(Throwable ex) {
    // Return cached product (if exist)
    Optional<Product> cache = cacheService.get("product:1");
    if (cache.isPresent()) {
        return toJson(cache.get());  // graceful degradation
    }
    throw ex;  // không có cache → propagate error
}
```

### 5. Timeout Tuning

**Rule of thumb:**

```
Service-level call:
- Network RTT: 10-50ms (local), 100-200ms (regional), 1000ms+ (international)
- Server processing: 50-500ms (normal), 1-5s (heavy), 5s+ (report generation)

Example tuning:
- Product service (simple lookup): timeout = 500ms (50ms RTT + 400ms processing + margin)
- Order service (complex business): timeout = 3s
- Report API (heavy): timeout = 30s

Do NOT:
- timeout < RTT (always fail)
- timeout = infinity (same as no timeout)
```

---

## Testing Guide

### Test 1: Retry Pattern

**Setup:**
```bash
# Terminal 1: Start Product service
cd product
mvn -Dspring.profiles.active=dev spring-boot:run

# Terminal 2: Start Order service
cd order
mvn -Dspring.profiles.active=dev spring-boot:run
```

**Test:**
```bash
# Request 1-3: Product service online → ok
curl http://localhost:8083/api/demo/resilience/retry

# Stop Product service (Ctrl+C in Terminal 1)

# Request 4: Product service offline → Retry 3 times → fail
curl http://localhost:8083/api/demo/resilience/retry
# Check log: see "Retry attempt 1", "Retry attempt 2", "Retry attempt 3"

# Start Product service again

# Request 5: Product service back online → ok
curl http://localhost:8083/api/demo/resilience/retry
```

**Expected Log:**
```
Retry 'productService' - Attempt 1 at ConnectException
Retry 'productService' - Attempt 2 at ConnectException
Retry 'productService' - Attempt 3 at ConnectException
Retry 'productService' - All attempts are exhausted
```

### Test 2: Circuit Breaker

**Test:**
```bash
# Simulate circuit breaker opening
for i in {1..15}; do
    echo "Request $i"
    curl http://localhost:8083/api/demo/resilience/circuit-breaker
    sleep 0.5
done
```

**Expected:**
- Request 1-6: fail (product service down)
- Request 7: Circuit OPEN
- Request 8-15: Circuit OPEN, fallback response immediately (không call product service)

### Test 3: Timeout

**Setup:**
```bash
# Mock slow endpoint in Product service:
@GetMapping("/api/products/slow")
public String slow() throws InterruptedException {
    Thread.sleep(10000);  // 10 seconds
    return "slow response";
}
```

**Test:**
```bash
curl http://localhost:8083/api/demo/resilience/timeout
# After 5s: TimeoutException (do not wait 10s)
```

### Test 4: Rate Limiter

**Test:**
```bash
# Burst 20 requests in <1s
for i in {1..20}; do curl -s http://localhost:8083/api/demo/resilience/rate-limit & done
wait

# Expected: 10 succeed, 10 fail with 429 Too Many Requests
```

### Test 5: Bulkhead

**Test:**
```bash
# 11 concurrent requests (simulate with background jobs)
for i in {1..11}; do 
    curl http://localhost:8083/api/demo/resilience/bulkhead &
done
wait

# Expected: 10 succeed, 1 fail with BulkheadFullException
```

---

## Summary

| Pattern | Goal | Config Key | When To Use |
|---------|------|-----------|-----------|
| **Retry** | Recover from transient failures | `max-attempts`, `wait-duration`, `multiplier` | Network glitch, temp timeout |
| **CircuitBreaker** | Fail-fast when service down | `failure-rate-threshold`, `wait-duration-in-open-state` | Downstream service protection |
| **Timeout** | Prevent hanging requests | `timeout-duration` | Unbounded network/DB calls |
| **RateLimiter** | Protect from overwhelming load | `limit-for-period`, `limit-refresh-period` | API public, bulk operations |
| **Bulkhead** | Isolate resource pools | `max-concurrent-calls`, `max-wait-duration` | Multiple endpoints compete for resource |

---

## Next Steps

1. ✅ **Implement** demo code vào Order service.
2. ✅ **Test** từng pattern riêng.
3. ✅ **Monitor** via logs + metrics.
4. ✅ **Tune** config theo actual production data.
5. ✅ **Document** fallback strategies cho team.

---

## References

- Resilience4j Docs: https://resilience4j.readme.io/
- Spring Boot Integration: https://docs.spring.io/spring-boot/reference/features/resilience4j.html
- Microservices Patterns (Sam Newman): https://microservices.io/patterns/resilience/index.html

