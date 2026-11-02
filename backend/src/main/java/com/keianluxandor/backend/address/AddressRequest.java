package com.keianluxandor.backend.address;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// The country decides the rules for phone, state and postal code; AddressService checks them.
// @JsonAlias: other field names the website may send for the same thing (e.g. "postalCode" for "pincode").
public record AddressRequest(

        @JsonAlias({"name"})
        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must be at most 100 characters")
        String fullName,

        // With country code, e.g. "+1 415 555 0100". A national number ("415 555 0100") is also
        // accepted for common countries: the country's dial code is added automatically.
        @JsonAlias({"phoneNumber", "mobile"})
        @NotBlank(message = "Phone is required")
        @Size(max = 25, message = "Phone is too long")
        String phone,

        @JsonAlias({"addressLine1", "street", "streetAddress"})
        @NotBlank(message = "Address line 1 is required")
        @Size(max = 200, message = "Address line 1 must be at most 200 characters")
        String line1,

        @JsonAlias({"addressLine2", "apartment"})
        @Size(max = 200, message = "Address line 2 must be at most 200 characters")
        String line2,

        @Size(max = 100, message = "Landmark must be at most 100 characters")
        String landmark,

        @NotBlank(message = "City is required")
        @Size(max = 80, message = "City must be at most 80 characters")
        String city,

        // State / province / region. Required for IN, US, CA and AU.
        @JsonAlias({"region", "province", "stateCode"})
        @Size(max = 100, message = "State must be at most 100 characters")
        String state,

        // Postal code / ZIP / PIN code. Not used in some countries (e.g. AE).
        @JsonAlias({"postalCode", "postcode", "zip", "zipCode"})
        @Size(max = 12, message = "Postal code is too long")
        String pincode,

        // 2-letter country code: US, GB, CA, AE, AU, IN ...
        @JsonAlias({"countryCode"})
        @NotBlank(message = "Country is required")
        @Size(max = 2, message = "Country must be a 2-letter code, e.g. US")
        String country,

        // Optional: true makes this the default address
        @JsonAlias({"isDefault"})
        Boolean makeDefault
) {
    public AddressRequest {
        fullName = trim(fullName);
        phone = trim(phone);
        line1 = trim(line1);
        line2 = blankToNull(line2);
        landmark = blankToNull(landmark);
        city = trim(city);
        state = blankToNull(state);
        pincode = blankToNull(pincode);
        country = trim(country);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}