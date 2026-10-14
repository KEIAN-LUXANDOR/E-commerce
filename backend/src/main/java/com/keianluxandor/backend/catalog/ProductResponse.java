package com.keianluxandor.backend.catalog;

import java.util.UUID;

public record ProductResponse(UUID id, String slug, String name, String description) {

    static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSlug(),
                product.getName(),
                product.getDescription());
    }
}