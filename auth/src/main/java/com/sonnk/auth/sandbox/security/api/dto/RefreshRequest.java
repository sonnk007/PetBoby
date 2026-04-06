package com.sonnk.auth.sandbox.security.api.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(

        @NotBlank(message = "refreshToken is required")
        String refreshToken
) {}
