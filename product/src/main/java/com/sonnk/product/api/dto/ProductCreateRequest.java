package com.sonnk.product.api.dto;

import com.sonnk.product.utils.enums.ProductSize;
import com.sonnk.product.utils.enums.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO cho API tạo mới sản phẩm.
 * - Dùng Bean Validation để validate input ở layer API.
 */
public record ProductCreateRequest(

        @NotBlank
        @Size(max = 50)
        String code,

        @NotBlank
        @Size(max = 150)
        String name,

        @Size(max = 1000)
        String description,

        @NotNull
        @DecimalMin("0.0")
        BigDecimal price,

        @NotNull
        ProductSize productSize,

        @NotNull
        Boolean hasTopping,

        @NotNull
        ProductStatus status,

        @NotNull
        Long categoryId,

        String imageUrl
) {
}

