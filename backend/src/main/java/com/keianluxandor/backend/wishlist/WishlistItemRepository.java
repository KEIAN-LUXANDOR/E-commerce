package com.keianluxandor.backend.wishlist;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, UUID> {

    // Newest first
    List<WishlistItem> findByUserIdOrderByCreatedAtDesc(UUID userId);

    boolean existsByUserIdAndProductId(UUID userId, UUID productId);

    long countByUserId(UUID userId);

    long deleteByUserIdAndProductId(UUID userId, UUID productId);
}