package com.keianluxandor.backend.catalog;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    List<ProductVariant> findByProductIdAndActiveTrueOrderBySortOrderAsc(UUID productId);

    List<ProductVariant> findByProductIdInAndActiveTrueOrderBySortOrderAsc(Collection<UUID> productIds);
}