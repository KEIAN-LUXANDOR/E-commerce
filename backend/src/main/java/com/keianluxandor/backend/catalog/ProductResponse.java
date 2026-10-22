package com.keianluxandor.backend.catalog;

import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String slug,
        String name,
        String description,
        DiamondResponse diamond,
        List<ProductImageResponse> images,
        List<ProductVariantResponse> variants) {

    // diamond can be null for a product that has no diamond details yet
    static ProductResponse from(Product product, DiamondDetails diamond,
                                List<ProductImage> images, List<ProductVariant> variants) {
        return new ProductResponse(
                product.getId(),
                product.getSlug(),
                product.getName(),
                product.getDescription(),
                diamond == null ? null : DiamondResponse.from(diamond),
                images.stream().map(ProductImageResponse::from).toList(),
                variants.stream().map(ProductVariantResponse::from).toList());
    }
}