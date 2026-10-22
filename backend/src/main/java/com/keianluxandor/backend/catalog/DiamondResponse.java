package com.keianluxandor.backend.catalog;

import java.math.BigDecimal;

public record DiamondResponse(
        DiamondGrades.Origin origin,
        DiamondGrades.Shape shape,
        BigDecimal carat,
        DiamondGrades.Color color,
        DiamondGrades.Clarity clarity,
        DiamondGrades.Cut cut,
        DiamondGrades.Finish polish,
        DiamondGrades.Finish symmetry,
        DiamondGrades.Fluorescence fluorescence,
        DiamondGrades.Lab lab,
        boolean quickShip) {

    static DiamondResponse from(DiamondDetails d) {
        return new DiamondResponse(
                d.getOrigin(),
                d.getShape(),
                d.getCarat(),
                d.getColor(),
                d.getClarity(),
                d.getCut(),
                d.getPolish(),
                d.getSymmetry(),
                d.getFluorescence(),
                d.getLab(),
                d.isQuickShip());
    }
}