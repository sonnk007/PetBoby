package com.sonnk.order.model.entity.enums;

/**
 * Demo Saga state machine cho luong choreography.
 *
 * Cong dung:
 * - Theo doi tien trinh xu ly giao dich phan tan (inventory -> payment -> confirm/cancel).
 * - Hien ro trang thai trung gian de debug eventual consistency.
 *
 * Han che:
 * - Skeleton nay la muc demo hoc tap, chua bao gom timeout scheduler/retry policy day du cho production.
 */
public enum OrderSagaState {
    NEW,
    INVENTORY_PENDING,
    INVENTORY_RESERVED,
    INVENTORY_FAILED,
    PAYMENT_PENDING,
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    COMPENSATING,
    COMPENSATED,
    COMPLETED
}
