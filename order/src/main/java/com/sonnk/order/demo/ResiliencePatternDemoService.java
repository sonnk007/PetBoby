package com.sonnk.order.demo;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;

/**
 * Resilience4j Demo Service - Phase 4
 *
 * Purpose:
 *   Minh hoạ cụ thể từng resilience pattern:
 *   - Retry: thử lại khi lỗi tạm thời
 *   - Circuit Breaker: mở circuit khi service gọi liên tục gặp lỗi
 *   - Timeout: đặt thời gian chờ tối đa
 *   - Rate Limiter: giới hạn request/giây để tránh quá tải
 *   - Bulkhead: giới hạn concurrent request
 *
 * Learning Points:
 *   - Kết hợp nhiều pattern để tạo hệ thống resilient
 *   - Fallback method để xử lý khi pattern kích hoạt
 *   - Configuration qua YAML (application.yml)
 */
@Slf4j
@Service
public class ResiliencePatternDemoService {

    private final RestTemplate restTemplate;

    public ResiliencePatternDemoService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Pattern 1: RETRY
     * 
     * Cơ chế:
     *   - Thử lại tối đa 3 lần nếu gặp lỗi (connection timeout, IOException).
     *   - Giữa mỗi attempt: backoff exponential (1s, 2s, 4s).
     *   - Không retry nếu gặp HTTP 4xx (client error).
     * 
     * Khi nào dùng:
     *   - Gọi API/DB bên ngoài có thể fail tạm thời.
     *   - Ví dụ: network hiếm hoi gặp hiccup, DB bị tải spike.
     * 
     * Thử test:
     *   1. Start product service.
     *   2. Gọi: GET /api/demo/resilience/retry
     *   3. Tắt product service giữa request → log sẽ show "Retry attempt 1/2/3".
     */
    @Retry(name = "productService")
    public String getProductWithRetry() {
        log.info("Attempting to call Product service (with retry)...");
        try {
            String response = restTemplate.getForObject(
                "http://localhost:8082/api/products/1",
                String.class
            );
            log.info("Product service responded successfully");
            return response;
        } catch (RestClientException e) {
            log.error("Product service call failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Pattern 2: CIRCUIT BREAKER
     *
     * Cơ chế:
     *   - CLOSED (bình thường): call qua bình thường.
     *   - OPEN (mở circuit): 50% trong 10 call gần nhất là lỗi → từ chối request ngay (fail-fast).
     *   - HALF_OPEN: sau 5s tự thử lại (test 3 call, nếu ok → CLOSED, nếu lỗi → OPEN lại).
     * 
     * Công dụng:
     *   - Tránh "cascade failure": order service chết → user service chờ mãi → gateway timeout.
     *   - Với circuit breaker: ngay khi order service liên tục lỗi → gateway từ chối request ngay.
     * 
     * Thử test:
     *   1. Gọi: GET /api/demo/resilience/circuit-breaker lặp lại
     *   2. Sau ~50% lỗi trong 10 call → log sẽ show "Circuit OPEN, rejecting request".
     */
    @CircuitBreaker(
        name = "productService",
        fallbackMethod = "fallbackGetProductCircuitBreaker"
    )
    public String getProductWithCircuitBreaker() {
        log.info("Calling Product service (with circuit breaker)...");
        try {
            String response = restTemplate.getForObject(
                "http://localhost:8082/api/products/1",
                String.class
            );
            log.info("Product service call succeeded");
            return response;
        } catch (RestClientException e) {
            log.error("Product service call failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Fallback method cho circuit breaker.
     * Tham số cuối cùng phải là Throwable (exception gây ra fallback).
     * 
     * Senior tip: fallback có thể:
     *   - Return cached data (nếu có).
     *   - Return default value.
     *   - Gọi fallback service (ví dụ: read cache, return "service unavailable" message).
     */
    public String fallbackGetProductCircuitBreaker(Throwable ex) {
        log.warn("Circuit breaker active! Using fallback response. Cause: {}", ex.getMessage());
        return """
            {"id": 0, "name": "FALLBACK", "status": "SERVICE_UNAVAILABLE", 
             "message": "Product service is currently unavailable, showing cached data"}
            """;
    }

    /**
     * Pattern 3: TIMEOUT (TimeLimiter)
     *
     * Cơ chế:
     *   - Đặt time limit 5 giây cho method.
     *   - Quá 5s → throw TimeoutException (regardless of actual I/O status).
     * 
     * Khi dùng:
     *   - Bảo vệ khỏi "hanging request" (client chờ mãi, server resource cạn).
     *   - Ví dụ: DB query kém hiệu suất chạy lâu → timeout thay vì chờ đợi.
     * 
     * Chú ý:
     *   - Nếu retry + timeout: timeout phải > mỗi retry attempt timeout.
     *   - Nếu kết hợp circuit breaker + timeout + retry: order thực hiện là
     *     timeLimiter(circuitBreaker(retry(...))).
     * 
     * Thử test:
     *   1. Mock endpoint ở product service cố tình delay 10s.
     *   2. Gọi endpoint này → sẽ timeout sau 5s.
     */
    @TimeLimiter(name = "productService")
    public CompletableFuture<String> getProductWithTimeout() {
        log.info("Calling Product service (with timeout limit)...");
        return CompletableFuture.supplyAsync(() -> {
            try {
                String response = restTemplate.getForObject(
                    "http://localhost:8082/api/products/1",
                    String.class
                );
                log.info("Product service responded within timeout");
                return response;
            } catch (RestClientException e) {
                log.error("Product service call failed: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Pattern 4: RATE LIMITER
     *
     * Cơ chế:
     *   - Giới hạn 10 request trong 1 giây cho bulk product API.
     *   - Nếu vượt giới hạn → reject request (throw RequestNotPermitted).
     * 
     * Khi dùng:
     *   - API public để tránh abuse.
     *   - Resource đắt đỏ (bulk upload, report generation).
     *   - Bảo vệ backend khỏi thundering herd (spike traffic).
     * 
     * Cảnh báo:
     *   - Rate limiter là per-instance (nếu có 3 server → mỗi cái được 10 req/s → total 30).
     *   - Để global rate limit → dùng API gateway (lua script ở redis).
     * 
     * Thử test:
     *   1. Gọi: GET /api/demo/resilience/rate-limit nhiều lần nhanh nhất có thể.
     *   2. Sau ~11 call trong 1s → sẽ fail (rate limit exceeded).
     */
    @RateLimiter(
        name = "productServiceBulk",
        fallbackMethod = "fallbackRateLimit"
    )
    public String bulkGetProducts() {
        log.info("Bulk get products (with rate limiter)...");
        return "Retrieving 100 products...";
    }

    /**
     * Fallback method cho rate limiter.
     * Gọi khi vượt quá giới hạn request/giây (> 10 req/s).
     * 
     * Senior tip: fallback có thể:
     *   - Return error message để client retry sau.
     *   - Queue request cho processing sau (nếu có queue).
     *   - Return cached/default result.
     */
    public String fallbackRateLimit(Throwable ex) {
        log.warn("Rate limit exceeded! Fallback activated. Cause: {}", ex.getMessage());
        return "Rate limit exceeded. Please retry after some time.";
    }

    /**
     * Pattern 5: BULKHEAD (Isolation / Thread Pool)
     *
     * Cơ chế:
     *   - Giới hạn tối đa 10 concurrent request cùng lúc cho method này.
     *   - Nếu có request thứ 11 → reject (fail with BulkheadFullException).
     * 
     * Công dụng:
     *   - Ngăn một API kém hiệu suất chặn các API khác.
     *   - Như các ngăn tàu: hỏng ngăn này không ảnh hưởng ngăn khác.
     * 
     * Ví dụ thực tế:
     *   - `/api/orders/analytics` (chạy lâu) không được chặn `/api/orders/123` (lấy chi tiết).
     *   - Nếu không bulkhead:
     *     - Mỗi request thread từ thread pool của Tomcat.
     *     - 200 request `/api/orders/analytics` (chạy 30s mỗi cái).
     *     - Thread pool cạn (vd: 200 threads) → `/api/orders/123` phải chờ.
     *   - Với bulkhead:
     *     - `/api/orders/analytics` chỉ được 10 thread.
     *     - Request thứ 11 → reject ngay → 190 thread còn lại cho `/api/orders/123`.
     * 
     * Thử test:
     *   1. Gọi endpoint này 11 lần đồng thời → 11 request thứ sẽ fail.
     */
    @Bulkhead(
        name = "productService",
        fallbackMethod = "fallbackBulkhead"
    )
    public String heavyAnalytics() {
        log.info("Running heavy analytics (with bulkhead isolation)...");
        try {
            Thread.sleep(2000); // Simulate heavy work
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "Analytics completed";
    }

    /**
     * Fallback method cho bulkhead.
     * Gọi khi số concurrent request vượt quá max-concurrent-calls (max 10).
     * 
     * Mục đích: Isolate heavy task từ service khác, không block fast endpoint.
     * 
     * Senior tip: fallback nên:
     *   - Từ chối request ngay (như hiện tại) -> client biết retry sau.
     *   - Hoặc queue request cho batch processing sau giờ cao điểm.
     */
    public String fallbackBulkhead(Throwable ex) {
        log.warn("Bulkhead capacity exceeded! Fallback activated. Cause: {}", ex.getMessage());
        return "Service too busy, please retry later";
    }

    /**
     * Pattern 6: COMBINED - Retry + Circuit Breaker + Timeout + Rate Limiter
     *
     * Cơ chế:
     *   - Kết hợp nhiều pattern để tạo end-to-end resilience.
     *   - Thứ tự Stack (từ bên ngoài vào trong):
     *     1. TimeLimiter: đặt timeout 5s
     *     2. CircuitBreaker: mở circuit nếu 50% lỗi
     *     3. Retry: thử lại tối đa 3 lần
     *     4. Actual method call
     * 
     * Ý tưởng:
     *   - Thời gian chờ tối đa (timeout) > từng retry attempt.
     *   - Nếu lỗi retry → circuit breaker tính vào count.
     *   - Nếu circuit mở → không thực hiện retry nữa.
     *
     * Senior mindset: 
     *   - Mỗi pattern có chi phí. Cân nhắc cái gì cần thiết.
     *   - Ví dụ: nếu không cần timeout → bỏ đi.
     *   - Log + metric để hiểu pattern nào đang hoạt động.
     */
    @Retry(name = "productService")
    @CircuitBreaker(
        name = "productService",
        fallbackMethod = "fallbackCombined"
    )
    @TimeLimiter(name = "productService")
    @RateLimiter(name = "productServiceBulk")
    public CompletableFuture<String> getProductCombined() {
        log.info("Calling Product service (with ALL resilience patterns combined)...");
        return CompletableFuture.supplyAsync(() -> {
            try {
                String response = restTemplate.getForObject(
                    "http://localhost:8082/api/products/1",
                    String.class
                );
                log.info("Combined resilience: Product service call succeeded");
                return response;
            } catch (RestClientException e) {
                log.error("Combined resilience: Product service call failed: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Fallback method cho combined patterns.
     * Gọi khi bất kỳ pattern nào trong stack kích hoạt (circuit breaker, timeout, rate limit, bulkhead).
     * 
     * Stack order: RateLimiter → Bulkhead → TimeLimiter → CircuitBreaker → Retry → actual call.
     * 
     * Senior tip: fallback phức tạp có thể:
     *   - Return cached version của product nếu có.
     *   - Call fallback service (replica, read-only backup).
     *   - Degrade gracefully (return partial data).
     */
    public CompletableFuture<String> fallbackCombined(Throwable ex) {
        log.warn("Combined patterns fallback triggered. Cause: {}", ex.getMessage());
        return CompletableFuture.completedFuture(
            "Fallback: Service temporarily unavailable"
        );
    }
}
