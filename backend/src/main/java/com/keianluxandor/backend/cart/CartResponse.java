package com.keianluxandor.backend.cart;

import java.util.List;

public record CartResponse(
        List<CartLineResponse> items,
        int itemCount,
        long subtotalPaise,
        boolean hasUnavailableItems) {
}