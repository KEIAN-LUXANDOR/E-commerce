package com.keianluxandor.backend.auth;

import java.util.Locale;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "Email is required")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(max = 72, message = "Password must be at most 72 characters")
        String password
) {
    public LoginRequest {
        email = (email == null) ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}