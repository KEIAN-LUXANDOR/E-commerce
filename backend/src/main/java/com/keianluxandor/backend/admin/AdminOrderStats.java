package com.keianluxandor.backend.admin;

import java.util.Map;

import com.keianluxandor.backend.order.OrderStatus;

// For admin tabs, e.g. "Paid (3)", and a warning when refunds are waiting
public record AdminOrderStats(
        Map<OrderStatus, Long> ordersByStatus,
        long paymentsToRefund) {
}