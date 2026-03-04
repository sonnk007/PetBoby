package com.sonnk.order.api.dto;

import com.sonnk.order.model.entity.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request tạo đơn hàng (POST /api/orders).
 * items: danh sách sản phẩm (productId + quantity); giá và tên lấy từ Product service.
 */
public record CreateOrderRequest(
        @NotBlank(message = "branchCode is required")
        String branchCode,

        Long customerId,

        @NotNull(message = "paymentMethod is required")
        PaymentMethod paymentMethod,

        @NotEmpty(message = "items cannot be empty")
        @Valid
        List<OrderItemRequest> items
) {
}
