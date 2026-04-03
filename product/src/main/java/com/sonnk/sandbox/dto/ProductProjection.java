package com.sonnk.sandbox.dto;

import java.math.BigDecimal;

/**
 * Lightweight projection of Product for list/summary views.
 * Reduces network bandwidth & memory by fetching only essential fields.
 * Used for read-only list operations where full entity is unnecessary.
 *
 * Benefits:
 * - Fetches only 3 columns (id, name, price) instead of 20+ from full entity
 * - No Hibernate dirty-checking overhead (immutable record)
 * - Reduced memory footprint (~85% smaller than full entity)
 * - Reduced network payload
 */
public record ProductProjection(
    Long id,
    String name,
    BigDecimal price
) {
}
