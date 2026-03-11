package com.sonnk.order.application.event.saga;

/**
 * Ket qua cuoi saga: order cancelled.
 */
public record OrderCancelledEvent(
        String eventId,
        Long orderId,
        String orderCode,
        String reason
) {
}
