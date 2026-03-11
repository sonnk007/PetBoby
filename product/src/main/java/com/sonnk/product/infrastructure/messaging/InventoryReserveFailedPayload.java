package com.sonnk.product.infrastructure.messaging;

/**
 * Payload product publish khi reserve inventory that bai.
 */
public record InventoryReserveFailedPayload(
        String eventId,
        Long orderId,
        String orderCode,
        String branchCode,
        String reason
) {
}
