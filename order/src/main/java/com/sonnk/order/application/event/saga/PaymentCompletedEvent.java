package com.sonnk.order.application.event.saga;

/**
 * Payment service thong bao thanh toan thanh cong.
 */
public record PaymentCompletedEvent(
        String eventId,
        Long orderId,
        String orderCode,
        String transactionRef
) {
}
