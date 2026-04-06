package com.sonnk.auth.sandbox.security.api.dto;

import jakarta.validation.constraints.NotBlank;

public record SandboxLoginRequest(

        @NotBlank(message = "username is required")
        String username,

        @NotBlank(message = "password is required")
        String password
) {}
