package com.sonnk.order.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Một dòng trong đơn: sản phẩm + số lượng. Giá và tên lấy từ Product service khi tạo đơn.
 */
public record OrderItemRequest(
        @NotNull(message = "productId is required")
        Long productId,

        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        Integer quantity
) {
}
