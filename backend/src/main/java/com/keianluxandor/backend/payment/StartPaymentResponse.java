package com.keianluxandor.backend.payment;

import java.time.Instant;

// Everything the website needs to open Razorpay Checkout
public record StartPaymentResponse(
        String keyId,
        String razorpayOrderId,
        long amountPaise,
        String currency,
        String orderNumber,
        Instant expiresAt,
        String customerName,
        String customerEmail,
        String customerPhone) {
}