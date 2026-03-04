package com.sonnk.order.application.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event publish lên Kafka khi đơn hàng được tạo (Tuần 4 – Event-driven intro).
 * Consumer (vd: product service) subscribe topic "order.created" để log hoặc xử lý (inventory, notification).
 * eventId: dùng làm idempotency key ở consumer (xem docs/kafka-data-integrity-and-deep-dive.md).
 */
public record OrderCreatedEvent(
        String eventId,
        Long orderId,
        String orderCode,
        String branchCode,
        Long customerId,
        BigDecimal totalAmount,
        BigDecimal finalAmount,
        LocalDateTime createdAt
) {
    public static OrderCreatedEvent of(Long orderId, String orderCode, String branchCode, Long customerId,
                                      BigDecimal totalAmount, BigDecimal finalAmount, LocalDateTime createdAt) {
        return new OrderCreatedEvent(UUID.randomUUID().toString(), orderId, orderCode, branchCode,
                customerId, totalAmount, finalAmount, createdAt);
    }
}
