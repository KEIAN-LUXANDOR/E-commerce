package com.keianluxandor.backend.catalog;

import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String slug,
        String name,
        String description,
        String currency,
        DiamondResponse diamond,
        List<ProductImageResponse> images,
        List<ProductVariantResponse> variants) {

    // diamond can be null for a product that has no diamond details yet.
    // currency: the store currency; variant prices are in its smallest unit (cents for USD).
    static ProductResponse from(Product product, DiamondDetails diamond,
                                List<ProductImage> images, List<ProductVariant> variants,
                                String currency) {
        return new ProductResponse(
                product.getId(),
                product.getSlug(),
                product.getName(),
                product.getDescription(),
                currency,
                diamond == null ? null : DiamondResponse.from(diamond),
                images.stream().map(ProductImageResponse::from).toList(),
                variants.stream().map(ProductVariantResponse::from).toList());
    }
}