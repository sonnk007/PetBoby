package com.sonnk.auth.sandbox.security.api.dto;

import com.sonnk.auth.sandbox.security.entity.SandboxRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "username is required")
        @Size(min = 3, max = 50, message = "username must be 3-50 characters")
        String username,

        @NotBlank(message = "password is required")
        @Size(min = 6, max = 100, message = "password must be 6-100 characters")
        String password,

        @NotNull(message = "role is required (USER or ADMIN)")
        SandboxRole role
) {}
