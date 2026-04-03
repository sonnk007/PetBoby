package com.sonnk.auth.api.dto;

/**
 * DTO for validation error details in ProblemDetail response.
 */
public record ValidationErrorDTO(
    String field,
    String message
) {
}
