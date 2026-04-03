package com.sonnk.sandbox.dto;

import java.math.BigDecimal;

/**
 * Projection of Order for list/summary views.
 * Used in order service to fetch only essential fields.
 */
public record OrderProjection(
    Long id,
    String code,
    BigDecimal totalAmount,
    BigDecimal totalDiscount,
    BigDecimal finalAmount,
    String status
) {
}
