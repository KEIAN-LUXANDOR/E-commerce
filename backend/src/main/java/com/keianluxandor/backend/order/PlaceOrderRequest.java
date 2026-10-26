package com.keianluxandor.backend.order;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record PlaceOrderRequest(
        @NotNull(message = "Choose a delivery address")
        UUID addressId
) {
}