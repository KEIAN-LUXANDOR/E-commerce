package com.keianluxandor.backend.admin;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.keianluxandor.backend.order.OrderItemResponse;
import com.keianluxandor.backend.order.OrderStatus;
import com.keianluxandor.backend.order.ShippingAddress;
import com.keianluxandor.backend.payment.PaymentStatus;

public record AdminOrderDetail(
        String orderNumber,
        OrderStatus status,
        Customer customer,
        long subtotalPaise,
        long shippingPaise,
        long discountPaise,
        long gstPaise,
        long totalPaise,
        String currency,
        ShippingAddress shippingAddress,
        List<OrderItemResponse> items,
        List<PaymentAttempt> payments,
        Instant createdAt,
        Instant expiresAt,
        Instant paidAt,
        Instant cancelledAt,
        String cancelReason) {

    public record Customer(UUID id, String fullName, String email, String phone) {
    }

    // One Razorpay attempt. razorpayPaymentId is set only when money was received.
    public record PaymentAttempt(
            int attemptNumber,
            String razorpayOrderId,
            String razorpayPaymentId,
            long amountPaise,
            PaymentStatus status,
            Instant createdAt) {
    }
}