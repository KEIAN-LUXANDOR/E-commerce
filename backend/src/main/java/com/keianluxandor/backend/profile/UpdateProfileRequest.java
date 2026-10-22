package com.keianluxandor.backend.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(

        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must be at most 100 characters")
        String fullName,

        // Optional: leave empty to remove the phone number
        @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Phone must be a 10-digit Indian mobile number")
        String phone
) {
    public UpdateProfileRequest {
        fullName = (fullName == null) ? null : fullName.trim();
        phone = (phone == null || phone.isBlank()) ? null : phone.trim();
    }
}