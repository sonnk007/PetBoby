package com.sonnk.product.infrastructure.messaging;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payload cho topic saga.order.created.
 */
public record SagaOrderCreatedPayload(
        String eventId,
        Long orderId,
        String orderCode,
        String branchCode,
        BigDecimal finalAmount,
        LocalDateTime createdAt
) {
}
