package com.keianluxandor.backend.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(

        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must be at most 100 characters")
        String fullName,

        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Phone must be a 10-digit Indian mobile number")
        String phone,

        @NotBlank(message = "Address line 1 is required")
        @Size(max = 200, message = "Address line 1 must be at most 200 characters")
        String line1,

        @Size(max = 200, message = "Address line 2 must be at most 200 characters")
        String line2,

        @Size(max = 100, message = "Landmark must be at most 100 characters")
        String landmark,

        @NotBlank(message = "City is required")
        @Size(max = 80, message = "City must be at most 80 characters")
        String city,

        @NotBlank(message = "State is required")
        String state,

        @NotBlank(message = "PIN code is required")
        @Pattern(regexp = "^[1-9][0-9]{5}$", message = "PIN code must be 6 digits and cannot start with 0")
        String pincode,

        // Optional: true makes this the default address
        Boolean makeDefault
) {
    public AddressRequest {
        fullName = trim(fullName);
        phone = trim(phone);
        line1 = trim(line1);
        line2 = blankToNull(line2);
        landmark = blankToNull(landmark);
        city = trim(city);
        state = trim(state);
        pincode = trim(pincode);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}