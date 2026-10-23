package com.keianluxandor.backend.catalog;

import java.util.UUID;

// A variant whose product is ACTIVE, with its live price and whether it can be bought right now.
// pricePaise is null if the variant itself was switched off.
public record SellableVariant(UUID variantId, ProductResponse product, Long pricePaise, boolean available) {
}