package com.sonnk.order.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Resilience4j Configuration - Phase 4: Resilience Patterns
 *
 * Purpose:
 *   - Tập trung cấu hình logging/monitoring cho Resilience4j patterns.
 *   - Các property (retry, circuit breaker, timeout, ...) được config trong application.yml.
 *
 * Learning Points:
 *   - Observability: log khi circuit breaker mở/đóng, retry chạy, để debug production issues.
 *   - Tách cấu hình (YAML) khỏi code → dễ thay đổi mà không build lại.
 */
@Slf4j
@Configuration
public class Resilience4jConfig {

    /**
     * Listener cho Circuit Breaker events (mở, đóng, reset, lỗi).
     * Khi debug: enable log level DEBUG để thấy rõ state transition.
     */
    @Bean
    public RegistryEventConsumer<CircuitBreaker> circuitBreakerEventConsumer() {
        return new RegistryEventConsumer<CircuitBreaker>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> entryAddedEvent) {
                CircuitBreaker circuitBreaker = entryAddedEvent.getAddedEntry();
                log.info("Circuit Breaker '{}' created", circuitBreaker.getName());

                circuitBreaker.getEventPublisher()
                    .onStateTransition(event ->
                        log.warn("Circuit Breaker '{}' state transition: {}",
                            circuitBreaker.getName(),
                            event)
                    )
                    .onSuccess(event ->
                        log.debug("Circuit Breaker '{}' recorded success", circuitBreaker.getName())
                    )
                    .onError(event ->
                        log.warn("Circuit Breaker '{}' recorded error: {}",
                            circuitBreaker.getName(),
                            event.getThrowable().getMessage())
                    );
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> entryReplacedEvent) {
                log.info("Circuit Breaker '{}' replaced", entryReplacedEvent.getAddedEntry().getName());
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> entryRemovedEvent) {
                log.info("Circuit Breaker '{}' removed", entryRemovedEvent.getRemovedEntry().getName());
            }
        };
    }

    /**
     * Listener cho Retry events.
     * Khi log: show retry attempt, wait time, để hiểu retry policy hiện tại.
     */
    @Bean
    public RegistryEventConsumer<Retry> retryEventConsumer() {
        return new RegistryEventConsumer<Retry>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<Retry> entryAddedEvent) {
                Retry retry = entryAddedEvent.getAddedEntry();
                log.info("Retry '{}' created", retry.getName());

                retry.getEventPublisher()
                    .onRetry(event ->
                        log.info("Retry '{}' - Attempt {} at {} (lastException: {})",
                            retry.getName(),
                            event.getNumberOfRetryAttempts(),
                            event.getLastThrowable().getClass().getSimpleName(),
                            event.getLastThrowable().getMessage())
                    )
                    .onSuccess(event ->
                        log.debug("Retry '{}' succeeded after {} attempts",
                            retry.getName(),
                            event.getNumberOfRetryAttempts())
                    )
                    .onError(event ->
                        log.error("Retry '{}' failed after {} attempts: {}",
                            retry.getName(),
                            event.getNumberOfRetryAttempts(),
                            event.getLastThrowable().getMessage())
                    );
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<Retry> entryReplacedEvent) {
                log.info("Retry '{}' replaced", entryReplacedEvent.getAddedEntry().getName());
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<Retry> entryRemovedEvent) {
                log.info("Retry '{}' removed", entryRemovedEvent.getRemovedEntry().getName());
            }
        };
    }
}
