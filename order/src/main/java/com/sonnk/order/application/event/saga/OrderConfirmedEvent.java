package com.sonnk.order.application.event.saga;

/**
 * Ket qua cuoi saga: order confirmed.
 */
public record OrderConfirmedEvent(
        String eventId,
        Long orderId,
        String orderCode
) {
}
