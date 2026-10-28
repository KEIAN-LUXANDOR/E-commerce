package com.keianluxandor.backend.order;

// What happened when a payment was confirmed for an order
public enum PaymentOutcome {
    PAID,          // the order is now paid
    ALREADY_PAID,  // the order was already paid before this payment
    NEEDS_REFUND   // the order can't take this payment (expired and a diamond was sold meanwhile)
}