package com.sonnk.order.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * Resilience4j Demo Controller - Phase 4
 *
 * Purpose:
 *   Expose endpoint để test từng resilience pattern.
 *   Base path: /api/demo/resilience/
 *
 * Test guide:
 *   1. Start Product service: mvn -Dspring.profiles.active=dev spring-boot:run (port 8082)
 *   2. Start Order service: mvn -Dspring.profiles.active=dev spring-boot:run (port 8083)
 *   3. Call từng endpoint để xem pattern hoạt động
 *   4. Enable DEBUG log để thấy rõ các state transition
 */
@Slf4j
@RestController
@RequestMapping("/api/demo/resilience")
public class ResiliencePatternDemoController {

    private final ResiliencePatternDemoService demoService;

    public ResiliencePatternDemoController(ResiliencePatternDemoService demoService) {
        this.demoService = demoService;
    }

    /**
     * Pattern 1: RETRY
     * 
     * Endpoint: GET /api/demo/resilience/retry
     * Hành vi: Thử lại của tối đa 3 lần khi gặp lỗi network.
     * 
     * Test:
     *   curl -X GET http://localhost:8083/api/demo/resilience/retry
     *   
     *   Nếu product service chạy: trả về product data (gặp lên từ product service).
     *   Nếu product service bị tắt: retry 3 lần rồi fail (sẽ thấy "Retry attempt 1/2/3 in log).
     */
    @GetMapping("/retry")
    public ResponseEntity<String> testRetry() {
        log.info("=== TEST RETRY PATTERN ===");
        log.info("Expected behavior: Retry up to 3 times on network errors");
        try {
            String response = demoService.getProductWithRetry();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Retry pattern exhausted after 3 attempts", e);
            return ResponseEntity.status(503)
                .body("Service unavailable after retries: " + e.getMessage());
        }
    }

    /**
     * Pattern 2: CIRCUIT BREAKER
     * 
     * Endpoint: GET /api/demo/resilience/circuit-breaker
     * Hành vi: Mở circuit nếu 50% trong 10 call gần nhất là lỗi.
     * 
     * Test (giả lập failure):
     *   1. Tắt product service.
     *   2. Gọi endpoint này 10 lần nhanh chóng (ví dụ: for i in {1..10}; do curl ...; done).
     *   3. Sau ~5-6 call → circuit mở → tiếp theo sẽ trả fallback response ngay (không gọi product service).
     *   4. Để reset: chờ 5 giây (automatic-transition auto sang HALF_OPEN), hoặc bật lại product service.
     *   
     *   curl -X GET http://localhost:8083/api/demo/resilience/circuit-breaker
     */
    @GetMapping("/circuit-breaker")
    public ResponseEntity<String> testCircuitBreaker() {
        log.info("=== TEST CIRCUIT BREAKER PATTERN ===");
        log.info("Expected behavior: Open circuit after 50% failure rate in 10 calls");
        try {
            String response = demoService.getProductWithCircuitBreaker();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Circuit breaker triggered fallback", e);
            return ResponseEntity.status(503)
                .body("Circuit breaker active: " + e.getMessage());
        }
    }

    /**
     * Pattern 3: TIMEOUT (TimeLimiter)
     * 
     * Endpoint: GET /api/demo/resilience/timeout
     * Hành vi: Timeout nếu quá 5 giây (independent of actual I/O).
     * 
     * Test (giả lập slow endpoint):
     *   1. Mock endpoint ở product service trả về delay 10s.
     *   2. Gọi endpoint này → sẽ timeout sau 5s.
     *   3. Sẽ thấy exception java.util.concurrent.TimeoutException.
     *   
     *   curl -X GET http://localhost:8083/api/demo/resilience/timeout
     */
    @GetMapping("/timeout")
    public ResponseEntity<String> testTimeout() {
        log.info("=== TEST TIMEOUT PATTERN ===");
        log.info("Expected behavior: TimeoutException if takes > 5 seconds");
        try {
            CompletableFuture<String> result = demoService.getProductWithTimeout();
            String response = result.get(); // Chờ result hoặc timeout exception
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Timeout pattern triggered", e);
            return ResponseEntity.status(503)
                .body("Request timeout after 5 seconds: " + e.getMessage());
        }
    }

    /**
     * Pattern 4: RATE LIMITER
     * 
     * Endpoint: GET /api/demo/resilience/rate-limit
     * Hành vi: Giới hạn 10 request trong 1 giây.
     * 
     * Test (cố gắng vượt qua rate limit):
     *   1. Gọi endpoint này 11 lần nhanh chóng trong 1 giây.
     *   2. Request thứ 11 sẽ fail (RequestNotPermitted exception).
     *   
     *   for i in {1..15}; do echo "Request $i"; curl -s http://localhost:8083/api/demo/resilience/rate-limit; echo; done
     */
    @GetMapping("/rate-limit")
    public ResponseEntity<String> testRateLimit() {
        log.info("=== TEST RATE LIMITER PATTERN ===");
        log.info("Expected behavior: Limit 10 requests per second");
        try {
            String response = demoService.bulkGetProducts();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Rate limit exceeded", e);
            return ResponseEntity.status(429) // Too Many Requests
                .body("Rate limit exceeded: " + e.getMessage());
        }
    }

    /**
     * Pattern 5: BULKHEAD
     * 
     * Endpoint: GET /api/demo/resilience/bulkhead
     * Hành vi: Giới hạn 10 concurrent request cùng lúc.
     * 
     * Test (giả lập concurrent load):
     *   1. Gọi endpoint này 11 lần đồng thời (sử dụng tool như Apache Bench hay JMeter).
     *   2. Request thứ 11 sẽ fail (BulkheadFullException).
     *   
     *   # Đơn giản: sử dụng background task
     *   for i in {1..12}; do curl -X GET http://localhost:8083/api/demo/resilience/bulkhead & done
     *   
     *   Bạn sẽ thấy:
     *   - 10 request thành công.
     *   - 2 request thất bại với "BulkheadFullException".
     */
    @GetMapping("/bulkhead")
    public ResponseEntity<String> testBulkhead() {
        log.info("=== TEST BULKHEAD PATTERN ===");
        log.info("Expected behavior: Limit 10 concurrent calls");
        try {
            String response = demoService.heavyAnalytics();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Bulkhead limit exceeded", e);
            return ResponseEntity.status(503)
                .body("Service too busy (bulkhead full): " + e.getMessage());
        }
    }

    /**
     * Pattern 6: COMBINED
     * 
     * Endpoint: GET /api/demo/resilience/combined
     * Hành vi: Kết hợp Retry + Circuit Breaker + Timeout + Rate Limiter.
     * 
     * Test:
     *   curl -X GET http://localhost:8083/api/demo/resilience/combined
     *   
     *   Simulated scenarios:
     *   - Scenario 1: Network glitch → Retry kích hoạt (3 attempts) → OK.
     *   - Scenario 2: Service down lâu → Circuit Breaker kích hoạt → Fallback.
     *   - Scenario 3: Slow endpoint → Timeout kích hoạt.
     *   - Scenario 4: Many requests/sec → Rate Limiter kích hoạt.
     */
    @GetMapping("/combined")
    public ResponseEntity<String> testCombined() {
        log.info("=== TEST COMBINED RESILIENCE PATTERNS ===");
        log.info("Expected behavior: Retry → Circuit Breaker → Timeout → Rate Limiter stack");
        try {
            CompletableFuture<String> result = demoService.getProductCombined();
            String response = result.get();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Combined patterns fallback triggered", e);
            return ResponseEntity.status(503)
                .body("Service unavailable with combined patterns: " + e.getMessage());
        }
    }

    /**
     * Info Endpoint: Xem current state của tất cả pattern
     * 
     * Endpoint: GET /api/demo/resilience/info
     * 
     * Trả về:
     *   - Current state của circuit breaker (CLOSED/OPEN/HALF_OPEN).
     *   - Recent failures tại pattern nào.
     *   - Suggestions cho debug.
     */
    @GetMapping("/info")
    public ResponseEntity<String> info() {
        return ResponseEntity.ok("""
            Resilience4j Demo - Pattern Information
            
            PATTERNS TESTED:
            1. RETRY: Auto-retry failed requests (exponential backoff)
            2. CIRCUIT BREAKER: Fail-fast when service is down
            3. TIMEOUT: Prevent hanging requests
            4. RATE LIMITER: Limit requests per time window
            5. BULKHEAD: Isolate concurrent calls
            6. COMBINED: Stack all patterns together
            
            AVAILABLE ENDPOINTS:
            - GET /api/demo/resilience/retry
            - GET /api/demo/resilience/circuit-breaker
            - GET /api/demo/resilience/timeout
            - GET /api/demo/resilience/rate-limit
            - GET /api/demo/resilience/bulkhead
            - GET /api/demo/resilience/combined
            - GET /api/demo/resilience/info
            
            DEBUG TIPS:
            1. Enable DEBUG logging: set log level to DEBUG in application.yml
            2. Monitor logs: check Circuit Breaker state transitions
            3. Test scenarios: stop product service to trigger patterns
            4. Check metrics: enable micrometer integration for detailed metrics
            """);
    }
}
