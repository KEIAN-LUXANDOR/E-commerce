package com.keianluxandor.backend.catalog;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

final class DiamondSpecifications {

    private DiamondSpecifications() {
    }

    // Products whose diamond matches every filter that was given
    static Specification<Product> matches(DiamondFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();

            if (filter.needsDiamondDetails()) {
                Subquery<UUID> sub = query.subquery(UUID.class);
                Root<DiamondDetails> d = sub.from(DiamondDetails.class);

                List<Predicate> where = new ArrayList<>();
                where.add(cb.equal(d.get("productId"), root.get("id")));

                if (filter.origin() != null) {
                    where.add(cb.equal(d.get("origin"), filter.origin()));
                }
                if (filter.minCarat() != null) {
                    where.add(cb.greaterThanOrEqualTo(d.<BigDecimal>get("carat"), filter.minCarat()));
                }
                if (filter.maxCarat() != null) {
                    where.add(cb.lessThanOrEqualTo(d.<BigDecimal>get("carat"), filter.maxCarat()));
                }
                if (filter.quickShipOnly()) {
                    where.add(cb.isTrue(d.<Boolean>get("quickShip")));
                }
                addIn(where, d, "shape", filter.shapes());
                addIn(where, d, "color", filter.colors());
                addIn(where, d, "clarity", filter.clarities());
                addIn(where, d, "cut", filter.cuts());
                addIn(where, d, "polish", filter.polishes());
                addIn(where, d, "symmetry", filter.symmetries());
                addIn(where, d, "fluorescence", filter.fluorescences());
                addIn(where, d, "lab", filter.labs());

                sub.select(d.<UUID>get("productId")).where(where.toArray(Predicate[]::new));
                conditions.add(cb.exists(sub));
            }

            if (filter.withImageOnly()) {
                Subquery<UUID> img = query.subquery(UUID.class);
                Root<ProductImage> i = img.from(ProductImage.class);
                img.select(i.<UUID>get("productId"))
                        .where(cb.equal(i.get("productId"), root.get("id")));
                conditions.add(cb.exists(img));
            }

            return cb.and(conditions.toArray(Predicate[]::new));
        };
    }

    // Adds "field IN (values)" only when the customer picked something
    private static void addIn(List<Predicate> where, Root<DiamondDetails> d, String field, Collection<?> values) {
        if (!values.isEmpty()) {
            where.add(d.get(field).in(values));
        }
    }
}