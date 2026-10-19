package com.keianluxandor.backend.auth;

import java.util.Locale;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// No role field on purpose: nobody can choose their own role.
public record RegisterRequest(

        @NotBlank(message = "Email is required")
        @Email(regexp = ".+@.+\\..+", message = "Enter a valid email address")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
        String password,

        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must be at most 100 characters")
        String fullName,

        @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Phone must be a 10-digit Indian mobile number")
        String phone
) {
    // Clean the input before validation runs
    public RegisterRequest {
        email = (email == null) ? null : email.trim().toLowerCase(Locale.ROOT);
        fullName = (fullName == null) ? null : fullName.trim();
        phone = (phone == null || phone.isBlank()) ? null : phone.trim();
    }
}