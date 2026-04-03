package com.sonnk.order.infrastructure.client;

import com.sonnk.order.application.dto.ProductInfoDto;
import com.sonnk.order.application.exception.ProductServiceUnavailableException;
import com.sonnk.order.application.port.ProductClient;
import com.sonnk.order.config.ProductServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Implementation của ProductClient bằng RestTemplate (Tuần 3 – Synchronous communication).
 *
 * Công dụng:
 * - Order service gọi Product service qua HTTP để lấy thông tin sản phẩm (giá, tên) khi tạo đơn.
 * - Timeout từ RestTemplateConfig tránh block vô hạn.
 * - 404 → Optional.empty(); timeout/5xx → ProductServiceUnavailableException.
 */
@Component
public class ProductRestClient implements ProductClient {

    private static final Logger log = LoggerFactory.getLogger(ProductRestClient.class);

    private final RestTemplate restTemplate;
    private final ProductServiceProperties props;
    private final Counter requestCounter;
    private final Counter successCounter;
    private final Counter failureCounter;
    private final Counter fallbackCounter;

    public ProductRestClient(@Qualifier("productServiceRestTemplate") RestTemplate restTemplate,
                            ProductServiceProperties props,
                            MeterRegistry meterRegistry) {
        this.restTemplate = restTemplate;
        this.props = props;
        this.requestCounter = meterRegistry.counter("product_client_requests_total");
        this.successCounter = meterRegistry.counter("product_client_success_total");
        this.failureCounter = meterRegistry.counter("product_client_failure_total");
        this.fallbackCounter = meterRegistry.counter("product_client_fallback_total");
    }

    private String baseUrl() {
        return props.getBaseUrl();
    }

    @Override
    @Retry(name = "productService")
    @CircuitBreaker(name = "productService", fallbackMethod = "fallbackGetProductById")
    public Optional<ProductInfoDto> getProductById(Long id) {
        String url = baseUrl() + "/api/products/" + id;
        requestCounter.increment();
        try {
            ProductInfoDto body = restTemplate.getForObject(url, ProductInfoDto.class);
            successCounter.increment();
            return Optional.ofNullable(body);
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            // 404 is considered a normal response (not a failure)
            return Optional.empty();
        } catch (ResourceAccessException e) {
            failureCounter.increment();
            log.warn("Product service unreachable for id={}: {}", id, e.getMessage());
            throw new ProductServiceUnavailableException("Product service unreachable: " + e.getMessage(), e);
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            failureCounter.increment();
            log.warn("Product service error for id={}: {} {}", id, e.getStatusCode(), e.getMessage());
            throw new ProductServiceUnavailableException("Product service error: " + e.getStatusCode(), e);
        }
    }

    // Fallback for circuit breaker when getProductById fails
    public Optional<ProductInfoDto> fallbackGetProductById(Long id, Throwable ex) {
        fallbackCounter.increment();
        log.warn("Fallback getProductById for id={} due to {}", id, ex.getMessage());
        return Optional.empty();
    }

    @Override
    @Retry(name = "productService")
    @CircuitBreaker(name = "productService", fallbackMethod = "fallbackGetProductsByIds")
    public List<ProductInfoDto> getProductsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        String idsParam = ids.stream().map(Object::toString).reduce((a, b) -> a + "," + b).orElse("");
        String url = baseUrl() + "/api/products/bulk?ids=" + idsParam;
        requestCounter.increment();
        try {
            ResponseEntity<List<ProductInfoDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );
            List<ProductInfoDto> result = response.getBody() != null ? response.getBody() : Collections.emptyList();
            successCounter.increment();
            return result;
        } catch (ResourceAccessException e) {
            failureCounter.increment();
            log.warn("Product service unreachable for bulk ids: {}", e.getMessage());
            throw new ProductServiceUnavailableException("Product service unreachable: " + e.getMessage(), e);
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            failureCounter.increment();
            log.warn("Product service error for bulk: {} {}", e.getStatusCode(), e.getMessage());
            throw new ProductServiceUnavailableException("Product service error: " + e.getStatusCode(), e);
        }
    }

    // Fallback for circuit breaker when bulk call fails
    public List<ProductInfoDto> fallbackGetProductsByIds(List<Long> ids, Throwable ex) {
        fallbackCounter.increment();
        log.warn("Fallback getProductsByIds for ids={} due to {}", ids, ex.getMessage());
        return Collections.emptyList();
    }
}
