package com.sonnk.order.application.event.saga;

/**
 * Inventory reserve that bai -> saga can cancel order ngay hoac kich hoat compensation khac.
 */
public record InventoryReserveFailedEvent(
        String eventId,
        Long orderId,
        String orderCode,
        String branchCode,
        String reason
) {
}
