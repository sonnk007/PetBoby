package com.sonnk.sandbox.db;

import java.math.BigDecimal;

/**
 * Exercise: DTO Projection for Product.
 * Only fetch essential fields for listing.
 */
public record ProductListDto(
    Long id,
    String name,
    BigDecimal price,
    String categoryName
) {}
