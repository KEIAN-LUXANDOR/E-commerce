package com.keianluxandor.backend.admin;

import java.time.Instant;

import com.keianluxandor.backend.order.OrderStatus;

public record AdminOrderSummary(
        String orderNumber,
        OrderStatus status,
        String customerName,
        String customerEmail,
        int itemCount,
        String firstItemName,
        long totalPaise,
        Instant createdAt,
        Instant paidAt) {
}