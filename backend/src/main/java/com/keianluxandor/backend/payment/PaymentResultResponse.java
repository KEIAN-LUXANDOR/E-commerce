package com.keianluxandor.backend.payment;

public record PaymentResultResponse(String orderNumber, PaymentStatus paymentStatus, String message) {

    static PaymentResultResponse of(String orderNumber, PaymentStatus status) {
        String message = switch (status) {
            case PAID -> "Payment received. Your order is confirmed.";
            case REFUND_REQUIRED -> "We received your payment, but this order had expired and a diamond in it "
                    + "was sold to another customer. Your payment will be refunded in full.";
            case CREATED -> "Payment is not complete yet.";
        };
        return new PaymentResultResponse(orderNumber, status, message);
    }
}