package com.keianluxandor.backend.auth;

import java.util.Locale;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailRequest(

        @NotBlank(message = "Email is required")
        @Email(regexp = ".+@.+\\..+", message = "Enter a valid email address")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email
) {
    public EmailRequest {
        email = (email == null) ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}