package com.sonnk.sandbox.dto;

/**
 * Lightweight projection of Category for reference in responses.
 */
public record CategoryProjection(
    Long id,
    String code,
    String name
) {
}
