package com.keianluxandor.backend.catalog;

import java.util.UUID;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

interface VariantStockRepository extends Repository<ProductVariant, UUID> {

    // Takes one piece in a single database step, so two buyers can never get the same diamond.
    // Returns 1 if reserved, 0 if it was already sold or switched off.
    @Modifying
    @Query(value = """
            UPDATE product_variants
            SET stock_quantity = stock_quantity - 1
            WHERE id = :id AND active AND stock_quantity >= 1
            """, nativeQuery = true)
    int takeOne(@Param("id") UUID variantId);

    // Puts one piece back (order cancelled or payment not completed)
    @Modifying
    @Query(value = "UPDATE product_variants SET stock_quantity = stock_quantity + 1 WHERE id = :id",
            nativeQuery = true)
    int putBackOne(@Param("id") UUID variantId);
}