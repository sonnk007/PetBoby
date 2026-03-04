package com.sonnk.order.api.dto;

import com.sonnk.order.model.entity.enums.OrderStatus;
import com.sonnk.order.model.entity.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response trả về đơn hàng (GET /api/orders, POST /api/orders).
 */
public record OrderResponse(
        Long id,
        String orderCode,
        String branchCode,
        Long customerId,
        OrderStatus status,
        PaymentMethod paymentMethod,
        BigDecimal totalAmount,
        BigDecimal totalDiscount,
        BigDecimal finalAmount,
        LocalDateTime createdAt,
        List<OrderItemResponse> items
) {
}
