package com.sonnk.product.infrastructure.messaging;

/**
 * Payload product publish khi reserve inventory thanh cong.
 */
public record InventoryReservedPayload(
        String eventId,
        Long orderId,
        String orderCode,
        String branchCode
) {
}
