package com.sonnk.auth.api.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mẫu error response chuẩn cho REST API auth (giống pattern ở các service khác).
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

