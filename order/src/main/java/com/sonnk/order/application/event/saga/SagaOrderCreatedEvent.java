package com.sonnk.order.application.event.saga;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event khoi tao Saga choreography sau khi order duoc tao.
 *
 * Cong dung:
 * - Phat dong buoc reserve inventory o service khac (product/inventory).
 * - Mang theo eventId de ho tro idempotency consumer.
 */
public record SagaOrderCreatedEvent(
        String eventId,
        Long orderId,
        String orderCode,
        String branchCode,
        BigDecimal finalAmount,
        LocalDateTime createdAt
) {
    public static SagaOrderCreatedEvent of(Long orderId,
                                           String orderCode,
                                           String branchCode,
                                           BigDecimal finalAmount,
                                           LocalDateTime createdAt) {
        return new SagaOrderCreatedEvent(UUID.randomUUID().toString(), orderId, orderCode, branchCode, finalAmount, createdAt);
    }
}
