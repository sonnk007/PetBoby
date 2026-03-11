package com.sonnk.order.application.event.saga;

/**
 * Payment that bai -> can kich hoat compensation.
 */
public record PaymentFailedEvent(
        String eventId,
        Long orderId,
        String orderCode,
        String reason
) {
}
