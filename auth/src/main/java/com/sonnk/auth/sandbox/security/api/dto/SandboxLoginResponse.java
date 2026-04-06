package com.sonnk.auth.sandbox.security.api.dto;

public record SandboxLoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,          // "Bearer"
        long expiresInSeconds
) {}
