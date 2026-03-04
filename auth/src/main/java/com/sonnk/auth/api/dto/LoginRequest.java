package com.sonnk.auth.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO cho request login.
 *
 * Rule: dùng Bean Validation để validate input, controller chỉ cần @Valid.
 */
public record LoginRequest(
        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        String password
) {
}

