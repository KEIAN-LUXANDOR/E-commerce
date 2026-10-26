package com.keianluxandor.backend.order;

import java.time.Instant;
import java.util.List;

public record OrderResponse(
        String orderNumber,
        OrderStatus status,
        long subtotalPaise,
        long shippingPaise,
        long discountPaise,
        long gstPaise,
        long totalPaise,
        String currency,
        ShippingAddress shippingAddress,
        List<OrderItemResponse> items,
        Instant createdAt,
        Instant expiresAt,
        Instant paidAt,
        Instant cancelledAt,
        String cancelReason) {

    static OrderResponse from(CustomerOrder order) {
        return new OrderResponse(
                order.getOrderNumber(),
                order.getStatus(),
                order.getSubtotalPaise(),
                order.getShippingPaise(),
                order.getDiscountPaise(),
                order.getGstPaise(),
                order.getTotalPaise(),
                order.getCurrency(),
                order.getShippingAddress(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getCreatedAt(),
                order.getExpiresAt(),
                order.getPaidAt(),
                order.getCancelledAt(),
                order.getCancelReason());
    }
}