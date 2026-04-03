---
title: Tóm tắt Resilience Patterns - Session PetBoby
date: 2026-03-19
---

# Tóm tắt kiến thức phiên: Resilience Patterns (PetBoby)

Ngày: 2026-03-19

## Mục tiêu
- Tổng hợp toàn bộ kiến thức đã học trong session này về Resilience Patterns.
- Ghi lại các thay đổi mã đã thực hiện trong dự án PetBoby.
- Cung cấp một ví dụ ứng dụng thực tế minh họa cách áp dụng những kiến thức này, kèm mục tiêu và tác dụng xuyên suốt.

## Kiến thức chính đã học

- Các pattern chính:
  - Retry: tự động thử lại khi có lỗi tạm thời.
  - Circuit Breaker: chặn cuộc gọi đến service bị lỗi để tránh sụp cascades.
  - TimeLimiter: giới hạn thời gian chờ cho cuộc gọi (đặc biệt với non-blocking).
  - Rate Limiter: giới hạn lưu lượng (edge hoặc per-service).
  - Bulkhead: cô lập tài nguyên để hạn chế tác động lan tỏa.

- Nguyên tắc đặt pattern:
  - Cheap checks ở Edge (gateway): block nhanh, giảm tải cho backend.
  - Resilience gần caller: đặt Retry/CircuitBreaker ở phía gọi (service client).
  - Observability: đo lường event (success/failure/fallback/rejected) bằng Micrometer + Prometheus.
  - Fallbacks: trả về giá trị an toàn (safe default) để hệ thống tiếp tục hoạt động.

- Công cụ / thư viện sử dụng:
  - Spring Boot 3.x
  - Resilience4j (Retry, CircuitBreaker, TimeLimiter, RateLimiter, Bulkhead)
  - Micrometer + Prometheus
  - Spring Cloud Gateway (edge)

## Những thay đổi mã đã thực hiện (vị trí & mục đích)

- Gateway (rate limiter + metrics):
  - [PetBoby/gateway-service/src/main/java](PetBoby/gateway-service/src/main/java#L1) — thêm `GatewayRateLimiterFilter` (in-memory token-bucket) và counters Micrometer (`gateway_rate_limiter_allowed_total`, `gateway_rate_limiter_rejected_total`).
  - [PetBoby/gateway-service/src/main/resources/application.yml](PetBoby/gateway-service/src/main/resources/application.yml#L1) — cấu hình `management.endpoints` để mở Prometheus.

- Order service (client resilience + observability):
  - [PetBoby/order/src/main/java/com/sonnk/order/infrastructure/client/ProductRestClient.java](PetBoby/order/src/main/java/com/sonnk/order/infrastructure/client/ProductRestClient.java#L1) — áp dụng `@Retry`, `@CircuitBreaker`, fallback methods, và Micrometer counters (`product_client_requests_total`, `product_client_success_total`, `product_client_failure_total`, `product_client_fallback_total`).
  - [PetBoby/order/src/main/java/com/sonnk/order/config/Resilience4jConfig.java](PetBoby/order/src/main/java/com/sonnk/order/config/Resilience4jConfig.java#L1) — đăng ký event listeners cho CircuitBreaker/Retry để log/metrics.
  - [PetBoby/order/src/main/resources/application.yml](PetBoby/order/src/main/resources/application.yml#L1) — cấu hình resilience4j và management endpoints.

- Pom / dependency updates:
  - [PetBoby/order/pom.xml](PetBoby/order/pom.xml#L1) — thêm Resilience4j modules, Micrometer, Actuator.
  - [PetBoby/gateway-service/pom.xml](PetBoby/gateway-service/pom.xml#L1) — thêm Micrometer, Actuator để expose Prometheus metrics.

## Vấn đề gặp phải & cách đã xử lý

- Lỗi compile: API thay đổi của Resilience4j (RegistryEventConsumer). Giải pháp: cập nhật `Resilience4jConfig` theo API mới (tên method đúng và log đơn giản hơn).
- Gateway `/actuator/prometheus` trả về 404 ban đầu: cần kiểm tra logs khởi động để xác định lý do (security, exposure, starter deprecation hoặc port conflict). (Pending debug tiếp theo).

## Hướng dẫn vận hành ngắn gọn

- Kiểm tra metrics Prometheus:

```powershell
Invoke-RestMethod 'http://localhost:8083/actuator/prometheus'
Invoke-RestMethod 'http://localhost:8080/actuator/prometheus'
```

- Kiểm tra log của gateway để xác định lý do endpoint khôngExpose nếu gặp 404.

## Ví dụ ứng dụng thực tế (minified) — Mục tiêu & Tác dụng

Mục tiêu: Bảo vệ `order` service khỏi lỗi và độ trễ của `product` service, đồng thời thu thập metric để giám sát hành vi (requests, failures, fallback). Giải pháp: đặt `Retry + CircuitBreaker` trên `ProductRestClient`, metric hóa kết quả, và đặt rate limiter ở gateway.

Code minh họa (cắt gọn) — `ProductRestClient`:

```java
// Ghi chú: đoạn này là minh họa; file thực tế nằm ở
// PetBoby/order/src/main/java/com/sonnk/order/infrastructure/client/ProductRestClient.java
@Service
public class ProductRestClient {
    private final RestTemplate rest; private final MeterRegistry meter;

    public ProductRestClient(RestTemplate rest, MeterRegistry meter) { this.rest = rest; this.meter = meter; }

    @Retry(name = "productService", fallbackMethod = "getProductByIdFallback")
    @CircuitBreaker(name = "productService", fallbackMethod = "getProductByIdFallback")
    public ProductDto getProductById(Long id) {
        meter.counter("product_client_requests_total").increment();
        ProductDto p = rest.getForObject("http://product/api/products/" + id, ProductDto.class);
        meter.counter("product_client_success_total").increment();
        return p;
    }

    public ProductDto getProductByIdFallback(Long id, Throwable t) {
        meter.counter("product_client_fallback_total").increment();
        return ProductDto.emptySafe();
    }
}
```

Xuyên suốt các kiến thức:
- Khi `product` có lỗi tạm thời: `Retry` sẽ thử lại theo cấu hình (small backoff).
- Nếu lỗi lặp lại hoặc độ trễ lâu: `CircuitBreaker` mở, cuộc gọi sẽ đi trực tiếp vào `getProductByIdFallback()` để tránh chồng chất lỗi.
- Mỗi lần success/failure/fallback được tăng counter Micrometer, export sang Prometheus để xem trend và cảnh báo.
- Gateway token-bucket rate limiter chặn lưu lượng vượt ngưỡng ở edge, giảm áp lực lên backend.

## Next steps / Recommendations

- Fix production concerns:
  - Thay in-memory gateway rate limiter bằng Redis (distributed) nếu cần quota toàn hệ thống.
  - Nếu muốn `TimeLimiter` non-blocking, chuyển `RestTemplate` sang `WebClient`.
  - Thêm integration tests (WireMock) cho `ProductRestClient` để assert fallback và metric increments.

- Kiểm tra và sửa Gateway `/actuator/prometheus` nếu vẫn 404: xem logs khởi động, kiểm tra `management.endpoints.web.exposure.include`, security config, và starter compatibility.

## Prometheus — Giải thích và vai trò trong Resilience Patterns

Prometheus là một hệ thống giám sát và thu thập metrics dạng pull-based: Prometheus định kỳ "scrape" endpoint (ví dụ `/actuator/prometheus`) để lấy số liệu dạng time-series. Trong ngữ cảnh Resilience Patterns, Prometheus được dùng để:

- Thu thập metric hành vi của các cơ chế resilience (Retry, CircuitBreaker, RateLimiter, Bulkhead).
- Quan sát trễ, tỉ lệ lỗi, số lần fallback, và số lần bị giới hạn (rejected) để ra quyết định vận hành.
- Cung cấp dữ liệu cho alerting (ví dụ: cảnh báo khi circuit open nhiều lần, hoặc fallback rate vượt ngưỡng).

Vai trò xuyên suốt từng khâu:
- Thiết kế / Local testing: dùng metric để kiểm tra rằng Retry/fallback hoạt động như mong muốn (ví dụ test mô phỏng lỗi và assert counter `product_client_fallback_total` tăng).
- Staging / Pre-production: kiểm tra behavioral metrics (error rate, request latency) để điều chỉnh thresholds của CircuitBreaker và RateLimiter.
- Production: giám sát thời gian thực để phát hiện sụp hoặc suy giảm — kết hợp alert rules và dashboard.

Ví dụ metric cụ thể (đã sử dụng trong session):
- `product_client_requests_total` — tổng cuộc gọi tới `product` từ `order`.
- `product_client_success_total` — tổng cuộc gọi thành công.
- `product_client_failure_total` — tổng lỗi (thrown exceptions).
- `product_client_fallback_total` — tổng lần fallback xảy ra.
- `gateway_rate_limiter_allowed_total` / `gateway_rate_limiter_rejected_total` — lưu lượng được cho phép/loại bỏ ở edge.

PromQL ví dụ để giám sát:

```
# Tỉ lệ fallback trong 5 phút
sum(increase(product_client_fallback_total[5m])) / sum(increase(product_client_requests_total[5m]))

# Số lỗi/giây trong 1 phút
sum(increase(product_client_failure_total[1m]))
```

Alert rule ý tưởng:
- Nếu `product_client_fallback_total` / `product_client_requests_total` > 0.1 trong 5 phút → tạo cảnh báo (tỉ lệ fallback quá cao).
- Nếu `gateway_rate_limiter_rejected_total` tăng đột ngột → điều tra lưu lượng bất thường hoặc tấn công.

Gợi ý dashboards:
- Panel latency (P50/P95/P99) cho request tới `product`.
- Panel error/fallback rate và circuit breaker state (mở/đóng) khi khả dụng.

Những lưu ý triển khai:
- Đặt tên metrics rõ ràng, bao gồm `service_client_metric` theo chuẩn (service hành vi + loại metric).
- Export các event quan trọng từ Resilience4j (circuit state change, retry attempts) thành counter/gauge để Prometheus thu thập.
- Với kiến trúc phân tán, kết hợp Prometheus với Grafana cho dashboard và Alertmanager cho cảnh báo.

---

File này do agent tạo tự động từ session chat; cần chỉnh nhỏ nếu muốn thêm log excerpts hoặc test commands cụ thể.
