package com.keianluxandor.backend.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// The 3 values Razorpay Checkout gives the browser after a successful payment
public record VerifyPaymentRequest(
        @NotBlank(message = "razorpayOrderId is required")
        @Size(max = 50, message = "razorpayOrderId is too long")
        String razorpayOrderId,

        @NotBlank(message = "razorpayPaymentId is required")
        @Size(max = 50, message = "razorpayPaymentId is too long")
        String razorpayPaymentId,

        @NotBlank(message = "razorpaySignature is required")
        @Size(max = 128, message = "razorpaySignature is too long")
        String razorpaySignature
) {
}