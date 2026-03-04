package com.sonnk.product.api.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mẫu error response chuẩn cho REST API.
 * Giúp client dễ parse và log lỗi, thay vì trả text tự do.
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

