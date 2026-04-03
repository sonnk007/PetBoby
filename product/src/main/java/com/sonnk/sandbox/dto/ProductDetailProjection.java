package com.sonnk.sandbox.dto;

import java.math.BigDecimal;

/**
 * Detailed projection of Product with category information for full-detail views.
 * Still reduces columns compared to full entity fetch.
 */
public record ProductDetailProjection(
    Long id,
    String code,
    String name,
    BigDecimal price,
    String categoryCode,
    String categoryName
) {
}
