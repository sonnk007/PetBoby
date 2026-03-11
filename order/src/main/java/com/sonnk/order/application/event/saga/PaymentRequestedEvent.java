package com.sonnk.order.application.event.saga;

import java.math.BigDecimal;

/**
 * Yeu cau payment service xu ly thanh toan (demo choreography).
 */
public record PaymentRequestedEvent(
        String eventId,
        Long orderId,
        String orderCode,
        String branchCode,
        BigDecimal amount
) {
}
