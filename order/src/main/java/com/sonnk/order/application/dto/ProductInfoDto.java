package com.sonnk.order.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * DTO thông tin sản phẩm nhận từ Product service (GET /api/products/{id} hoặc /api/products/bulk).
 * Chỉ chứa các field cần cho Order: snapshot giá, tên, size khi tạo đơn.
 * productSize/status là String để deserialize từ JSON mà không phụ thuộc module product.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductInfoDto(
        Long id,
        String code,
        String name,
        BigDecimal price,
        String productSize,
        String status
) {
}
