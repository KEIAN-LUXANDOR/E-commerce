package com.keianluxandor.backend.order;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// One diamond in an order, with a copy of its name and price at the time of ordering
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "line_number", nullable = false, updatable = false)
    private int lineNumber;

    @Column(name = "product_id", updatable = false)
    private UUID productId;

    @Column(name = "variant_id", updatable = false)
    private UUID variantId;

    @Column(name = "product_slug", nullable = false, length = 200, updatable = false)
    private String productSlug;

    @Column(name = "product_name", nullable = false, length = 200, updatable = false)
    private String productName;

    @Column(nullable = false, length = 100, updatable = false)
    private String sku;

    // Short grade summary, e.g. "2.01 ct · F · VS1 · IGI"
    @Column(length = 200, updatable = false)
    private String description;

    @Column(name = "image_url", length = 1000, updatable = false)
    private String imageUrl;

    @Column(name = "price_paise", nullable = false, updatable = false)
    private long pricePaise;

    protected OrderItem() {
    }

    OrderItem(int lineNumber, UUID productId, UUID variantId, String productSlug, String productName,
              String sku, String description, String imageUrl, long pricePaise) {
        this.lineNumber = lineNumber;
        this.productId = productId;
        this.variantId = variantId;
        this.productSlug = productSlug;
        this.productName = productName;
        this.sku = sku;
        this.description = description;
        this.imageUrl = imageUrl;
        this.pricePaise = pricePaise;
    }

    public UUID getId() { return id; }
    public int getLineNumber() { return lineNumber; }
    public UUID getProductId() { return productId; }
    public UUID getVariantId() { return variantId; }
    public String getProductSlug() { return productSlug; }
    public String getProductName() { return productName; }
    public String getSku() { return sku; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public long getPricePaise() { return pricePaise; }
}