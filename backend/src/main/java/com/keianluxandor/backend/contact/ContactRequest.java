package com.keianluxandor.backend.contact;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// What the "Contact us" form sends
public record ContactRequest(

        @JsonAlias({"fullName"})
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(regexp = ".+@.+\\..+", message = "Enter a valid email address")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email,

        @JsonAlias({"phoneNumber", "mobile"})
        @Size(max = 25, message = "Phone is too long")
        String phone,

        @Size(max = 150, message = "Subject must be at most 150 characters")
        String subject,

        @JsonAlias({"order", "orderId"})
        @Size(max = 20, message = "Order number is too long")
        String orderNumber,

        @JsonAlias({"body", "comment", "comments"})
        @NotBlank(message = "Message is required")
        @Size(min = 10, max = 5000, message = "Message must be 10 to 5000 characters")
        String message,

        // Hidden field ("honeypot"): people never see it, bots fill it in. If it has a value, the message is ignored.
        String website
) {
    public ContactRequest {
        name = singleLine(name);
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        phone = blankToNull(phone);
        subject = blankToNull(singleLine(subject));
        orderNumber = blankToNull(orderNumber == null ? null : orderNumber.trim().toUpperCase(Locale.ROOT));
        message = message == null ? null : message.trim();
    }

    private static String singleLine(String value) {
        return value == null ? null : value.replaceAll("[\\r\\n]+", " ").trim();
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}