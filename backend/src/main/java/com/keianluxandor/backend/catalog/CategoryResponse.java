package com.keianluxandor.backend.catalog;

import java.util.UUID;

public record CategoryResponse(UUID id, String slug, String name) {

    static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getSlug(), category.getName());
    }
}