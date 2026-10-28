package com.keianluxandor.backend.payment;

public enum PaymentStatus {
    CREATED,         // Razorpay order created, customer hasn't paid (yet)
    PAID,            // payment received and the order is paid
    REFUND_REQUIRED  // money received but the order couldn't take it: must be refunded
}