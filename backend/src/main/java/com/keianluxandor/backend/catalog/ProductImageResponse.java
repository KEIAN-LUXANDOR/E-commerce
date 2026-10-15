package com.keianluxandor.backend.catalog;

public record ProductImageResponse(String url, String altText) {

    static ProductImageResponse from(ProductImage image) {
        return new ProductImageResponse(image.getUrl(), image.getAltText());
    }
}