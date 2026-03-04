package com.sonnk.auth.api.dto;

/**
 * DTO trả về cho client sau khi login thành công.
 */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds
) {
}

