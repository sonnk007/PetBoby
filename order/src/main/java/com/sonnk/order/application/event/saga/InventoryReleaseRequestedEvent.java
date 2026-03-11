package com.sonnk.order.application.event.saga;

/**
 * Compensation event: yeu cau inventory service release stock da reserve truoc do.
 */
public record InventoryReleaseRequestedEvent(
        String eventId,
        Long orderId,
        String orderCode,
        String reason
) {
}
