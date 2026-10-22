package com.keianluxandor.backend.catalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

// Diamond filters from the shop page. An empty list or null means "any".
public record DiamondFilter(
        DiamondGrades.Origin origin,
        List<DiamondGrades.Shape> shapes,
        BigDecimal minCarat,
        BigDecimal maxCarat,
        List<DiamondGrades.Color> colors,
        List<DiamondGrades.Clarity> clarities,
        List<DiamondGrades.Cut> cuts,
        List<DiamondGrades.Finish> polishes,
        List<DiamondGrades.Finish> symmetries,
        List<DiamondGrades.Fluorescence> fluorescences,
        List<DiamondGrades.Lab> labs,
        boolean withImageOnly,
        boolean quickShipOnly) {

    public DiamondFilter {
        shapes = clean(shapes);
        colors = clean(colors);
        clarities = clean(clarities);
        cuts = clean(cuts);
        polishes = clean(polishes);
        symmetries = clean(symmetries);
        fluorescences = clean(fluorescences);
        labs = clean(labs);
    }

    // True if any filter needs the diamond_details table
    boolean needsDiamondDetails() {
        return origin != null || minCarat != null || maxCarat != null || quickShipOnly
                || !shapes.isEmpty() || !colors.isEmpty() || !clarities.isEmpty() || !cuts.isEmpty()
                || !polishes.isEmpty() || !symmetries.isEmpty() || !fluorescences.isEmpty() || !labs.isEmpty();
    }

    // Removes empty values (e.g. "color=") and duplicates
    private static <T> List<T> clean(List<T> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().filter(Objects::nonNull).distinct().toList();
    }
}