package com.keianluxandor.backend.cart;

import java.time.Instant;
import java.util.UUID;

import com.keianluxandor.backend.catalog.ProductResponse;

// One diamond in the cart. available = false means sold or switched off (not counted in the total).
public record CartLineResponse(
        UUID variantId,
        ProductResponse product,
        Long pricePaise,
        boolean available,
        Instant addedAt) {
}