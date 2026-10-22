package com.keianluxandor.backend.catalog;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "diamond_details")
public class DiamondDetails {

    // Same id as the product (one diamond row per product)
    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DiamondGrades.Origin origin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DiamondGrades.Shape shape;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal carat;

    // columnDefinition: Hibernate would otherwise expect CHAR(1) for a length-1 column
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(1)")
    private DiamondGrades.Color color;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private DiamondGrades.Clarity clarity;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private DiamondGrades.Cut cut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DiamondGrades.Finish polish;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DiamondGrades.Finish symmetry;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private DiamondGrades.Fluorescence fluorescence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private DiamondGrades.Lab lab;

    @Column(name = "quick_ship", nullable = false)
    private boolean quickShip;

    protected DiamondDetails() {
    }

    public UUID getProductId() { return productId; }
    public DiamondGrades.Origin getOrigin() { return origin; }
    public DiamondGrades.Shape getShape() { return shape; }
    public BigDecimal getCarat() { return carat; }
    public DiamondGrades.Color getColor() { return color; }
    public DiamondGrades.Clarity getClarity() { return clarity; }
    public DiamondGrades.Cut getCut() { return cut; }
    public DiamondGrades.Finish getPolish() { return polish; }
    public DiamondGrades.Finish getSymmetry() { return symmetry; }
    public DiamondGrades.Fluorescence getFluorescence() { return fluorescence; }
    public DiamondGrades.Lab getLab() { return lab; }
    public boolean isQuickShip() { return quickShip; }
}