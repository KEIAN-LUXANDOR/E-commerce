package com.keianluxandor.backend.catalog;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
        boolean quickShip,
        String certificateNumber,
        String verifyUrl,
        String growthType,
        BigDecimal lengthMm,
        BigDecimal widthMm,
        BigDecimal depthMm,
        BigDecimal lwRatio,
        BigDecimal tablePct,
        BigDecimal depthPct,
        String videoUrl) {

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
                d.isQuickShip(),
                d.getCertificateNumber(),
                d.getCertificateNumber() == null ? null : verifyPage(d.getLab()),
                d.getGrowthType(),
                d.getLengthMm(),
                d.getWidthMm(),
                d.getDepthMm(),
                lengthToWidth(d.getLengthMm(), d.getWidthMm()),
                d.getTablePct(),
                d.getDepthPct(),
                d.getVideoUrl());
    }

    // The lab's official page where a customer types the report number
    private static String verifyPage(DiamondGrades.Lab lab) {
        return switch (lab) {
            case GIA -> "https://www.gia.edu/report-check";
            case IGI -> "https://www.igi.org/verify-your-report/";
            case HRD -> null;
        };
    }

    // L/W ratio, e.g. 7.84 / 5.62 = 1.40
    private static BigDecimal lengthToWidth(BigDecimal length, BigDecimal width) {
        if (length == null || width == null || width.signum() == 0) {
            return null;
        }
        return length.divide(width, 2, RoundingMode.HALF_UP);
    }
}