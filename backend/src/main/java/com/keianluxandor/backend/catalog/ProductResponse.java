package com.keianluxandor.backend.catalog;

import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String slug,
        String name,
        String description,
        List<ProductImageResponse> images) {

    static ProductResponse from(Product product, List<ProductImage> images) {
        return new ProductResponse(
                product.getId(),
                product.getSlug(),
                product.getName(),
                product.getDescription(),
                images.stream().map(ProductImageResponse::from).toList());
    }
}