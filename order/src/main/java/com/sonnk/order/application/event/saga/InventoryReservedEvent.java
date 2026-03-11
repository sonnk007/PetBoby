package com.sonnk.order.application.event.saga;

/**
 * Inventory reserve thanh cong.
 */
public record InventoryReservedEvent(
        String eventId,
        Long orderId,
        String orderCode,
        String branchCode
) {
}
