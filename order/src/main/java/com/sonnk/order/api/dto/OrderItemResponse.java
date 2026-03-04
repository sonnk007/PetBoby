package com.sonnk.order.api.dto;

import java.math.BigDecimal;

/**
 * Một dòng trong response đơn hàng.
 */
public record OrderItemResponse(
        Long id,
        Long productId,
        String productName,
        String productSize,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal lineTotal
) {
}
