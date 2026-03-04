package com.sonnk.product.api.dto;

import com.sonnk.product.utils.enums.ProductSize;
import com.sonnk.product.utils.enums.ProductStatus;

import java.math.BigDecimal;

/**
 * DTO trả về cho client khi xem thông tin sản phẩm.
 * - Tách khỏi entity để tránh lộ cấu trúc DB và dễ thay đổi sau này.
 */
public record ProductResponse(
        Long id,
        String code,
        String name,
        String description,
        BigDecimal price,
        ProductSize productSize,
        Boolean hasTopping,
        ProductStatus status,
        Long categoryId,
        String categoryCode,
        String categoryName,
        String imageUrl
) {
}

