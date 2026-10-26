package com.keianluxandor.backend.order;

public record OrderItemResponse(
        int lineNumber,
        String productSlug,
        String productName,
        String sku,
        String description,
        String imageUrl,
        long pricePaise) {

    static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getLineNumber(),
                item.getProductSlug(),
                item.getProductName(),
                item.getSku(),
                item.getDescription(),
                item.getImageUrl(),
                item.getPricePaise());
    }
}