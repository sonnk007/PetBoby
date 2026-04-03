package com.sonnk.order.api.dto;

/**
 * DTO for validation error details in ProblemDetail response.
 */
public record ValidationErrorDTO(
    String field,
    String message
) {
}
