package com.sonnk.order.api.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO chuẩn cho error response REST API (theo rule Clean API).
 * Giúp client parse và log lỗi thống nhất; dùng trong GlobalExceptionHandler.
 */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> validationErrors
) {
}
