package com.keianluxandor.backend.catalog;

import java.util.UUID;

public record ProductVariantResponse(
        UUID id,
        String sku,
        String name,
        long pricePaise,
        Long mrpPaise,
        boolean inStock) {

    static ProductVariantResponse from(ProductVariant variant) {
        return new ProductVariantResponse(
                variant.getId(),
                variant.getSku(),
                variant.getName(),
                variant.getPricePaise(),
                variant.getMrpPaise(),
                variant.getStockQuantity() > 0);
    }
}