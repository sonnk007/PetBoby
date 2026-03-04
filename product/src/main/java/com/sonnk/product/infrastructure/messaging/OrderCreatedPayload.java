package com.sonnk.product.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payload nhận từ Kafka topic "order.created" (Tuần 4 – Event-driven intro).
 * Cùng cấu trúc với OrderCreatedEvent bên order service để JSON deserialize đúng.
 * eventId: dùng làm idempotency key (check processed_events trước khi xử lý – xem docs/kafka-data-integrity-and-deep-dive.md).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderCreatedPayload(
        String eventId,
        Long orderId,
        String orderCode,
        String branchCode,
        Long customerId,
        BigDecimal totalAmount,
        BigDecimal finalAmount,
        LocalDateTime createdAt
) {
}
