package com.keianluxandor.backend.cart;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    List<CartItem> findByUserIdOrderByCreatedAtDescIdAsc(UUID userId);

    boolean existsByUserIdAndVariantId(UUID userId, UUID variantId);

    long countByUserId(UUID userId);

    // Adds the item; does nothing if it's already in the cart (e.g. clicked in two tabs)
    @Modifying
    @Query(value = """
            INSERT INTO cart_items (id, user_id, variant_id, created_at)
            VALUES (gen_random_uuid(), :userId, :variantId, now())
            ON CONFLICT (user_id, variant_id) DO NOTHING
            """, nativeQuery = true)
    int addIfAbsent(@Param("userId") UUID userId, @Param("variantId") UUID variantId);

    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.userId = :userId AND c.variantId = :variantId")
    int removeItem(@Param("userId") UUID userId, @Param("variantId") UUID variantId);

    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.userId = :userId")
    int clear(@Param("userId") UUID userId);
}