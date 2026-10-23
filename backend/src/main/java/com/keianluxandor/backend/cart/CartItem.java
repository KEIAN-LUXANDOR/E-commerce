package com.keianluxandor.backend.cart;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Rows are added with CartItemRepository.addIfAbsent, so this entity is read-only
@Entity
@Table(name = "cart_items")
public class CartItem {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "variant_id", nullable = false, updatable = false)
    private UUID variantId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected CartItem() {
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getVariantId() {
        return variantId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}