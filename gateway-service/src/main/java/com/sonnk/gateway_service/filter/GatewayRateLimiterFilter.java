package com.sonnk.gateway_service.filter;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Simple in-memory per-client token-bucket rate limiter for demo purposes.
 * - Capacity: 10 tokens
 * - Refill: 10 tokens per second
 *
 * Note: per-instance only. For production use Redis/Distributed rate limiter.
 */
@Component
public class GatewayRateLimiterFilter implements GlobalFilter, Ordered {

    private static final long CAPACITY = 10L;
    private static final long REFILL_PER_SECOND = 10L;

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final Counter allowedCounter;
    private final Counter rejectedCounter;

    public GatewayRateLimiterFilter(MeterRegistry meterRegistry) {
        this.allowedCounter = meterRegistry.counter("gateway_rate_limiter_allowed_total");
        this.rejectedCounter = meterRegistry.counter("gateway_rate_limiter_rejected_total");
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientKey = extractClientKey(exchange);
        TokenBucket bucket = buckets.computeIfAbsent(clientKey, k -> new TokenBucket(CAPACITY, REFILL_PER_SECOND));

        synchronized (bucket) {
            long now = Instant.now().getEpochSecond();
            bucket.refill(now);
            if (bucket.consume()) {
                allowedCounter.increment();
                return chain.filter(exchange);
            }
        }

        rejectedCounter.increment();
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        byte[] bytes = "Rate limit exceeded. Please retry later.".getBytes();
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }

    private String extractClientKey(ServerWebExchange exchange) {
        // Prefer X-Forwarded-For header set by upstream; fallback to remote address
        String xff = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        if (exchange.getRequest().getRemoteAddress() != null) {
            return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown-client";
    }

    @Override
    public int getOrder() {
        // Run early in filter chain
        return -100;
    }

    private static class TokenBucket {
        private final long capacity;
        private final long refillPerSecond;
        private long tokens;
        private long lastRefillEpochSec;

        TokenBucket(long capacity, long refillPerSecond) {
            this.capacity = capacity;
            this.refillPerSecond = refillPerSecond;
            this.tokens = capacity;
            this.lastRefillEpochSec = Instant.now().getEpochSecond();
        }

        void refill(long nowEpochSec) {
            long elapsed = nowEpochSec - lastRefillEpochSec;
            if (elapsed > 0) {
                long refill = elapsed * refillPerSecond;
                tokens = Math.min(capacity, tokens + refill);
                lastRefillEpochSec = nowEpochSec;
            }
        }

        boolean consume() {
            if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }
    }
}
