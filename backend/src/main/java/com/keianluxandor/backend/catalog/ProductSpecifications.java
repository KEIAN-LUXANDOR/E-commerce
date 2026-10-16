package com.keianluxandor.backend.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

final class ProductSpecifications {

    private ProductSpecifications() {
    }

    static Specification<Product> isActive() {
        return (root, query, cb) -> cb.equal(root.get("status"), ProductStatus.ACTIVE);
    }

    static Specification<Product> nameContains(String text) {
        return (root, query, cb) -> {
            if (text == null || text.isBlank()) {
                return cb.conjunction();
            }
            String pattern = "%" + escapeLike(text.trim().toLowerCase()) + "%";
            return cb.like(cb.lower(root.<String>get("name")), pattern, '\\');
        };
    }

    static Specification<Product> inCategory(String categorySlug) {
        return (root, query, cb) -> {
            if (categorySlug == null || categorySlug.isBlank()) {
                return cb.conjunction();
            }
            Subquery<UUID> categoryIds = query.subquery(UUID.class);
            Root<Category> category = categoryIds.from(Category.class);
            categoryIds.select(category.<UUID>get("id"))
                    .where(cb.equal(category.get("slug"), categorySlug.trim()));
            return root.<UUID>get("categoryId").in(categoryIds);
        };
    }

    static Specification<Product> priceBetween(Long minPaise, Long maxPaise) {
        return (root, query, cb) -> {
            if (minPaise == null && maxPaise == null) {
                return cb.conjunction();
            }
            Subquery<UUID> matchingVariants = query.subquery(UUID.class);
            Root<ProductVariant> variant = matchingVariants.from(ProductVariant.class);

            List<Predicate> conditions = new ArrayList<>();
            conditions.add(cb.equal(variant.get("productId"), root.get("id")));
            conditions.add(cb.isTrue(variant.<Boolean>get("active")));
            if (minPaise != null) {
                conditions.add(cb.greaterThanOrEqualTo(variant.<Long>get("pricePaise"), minPaise));
            }
            if (maxPaise != null) {
                conditions.add(cb.lessThanOrEqualTo(variant.<Long>get("pricePaise"), maxPaise));
            }

            matchingVariants.select(variant.<UUID>get("id"))
                    .where(conditions.toArray(new Predicate[0]));
            return cb.exists(matchingVariants);
        };
    }

    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}