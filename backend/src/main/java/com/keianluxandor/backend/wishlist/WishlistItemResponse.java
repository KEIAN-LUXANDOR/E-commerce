package com.keianluxandor.backend.wishlist;

import java.time.Instant;

import com.keianluxandor.backend.catalog.ProductResponse;

// One wishlist entry: the full product (same shape as the product API) and when it was added
public record WishlistItemResponse(ProductResponse product, Instant addedAt) {
}