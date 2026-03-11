package com.sonnk.product.infrastructure.messaging;

/**
 * Compensation payload: yeu cau giai phong inventory da reserve.
 */
public record InventoryReleaseRequestedPayload(
        String eventId,
        Long orderId,
        String orderCode,
        String reason
) {
}
