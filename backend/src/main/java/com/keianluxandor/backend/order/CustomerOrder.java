package com.keianluxandor.backend.order;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class CustomerOrder {

    public static final String CURRENCY = "INR";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_number", nullable = false, unique = true, length = 20, updatable = false)
    private String orderNumber;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "subtotal_paise", nullable = false)
    private long subtotalPaise;

    @Column(name = "shipping_paise", nullable = false)
    private long shippingPaise;

    @Column(name = "discount_paise", nullable = false)
    private long discountPaise;

    // GST is 0 for now; prices are treated as final. Can be calculated later.
    @Column(name = "gst_paise", nullable = false)
    private long gstPaise;

    @Column(name = "total_paise", nullable = false)
    private long totalPaise;

    @Column(nullable = false, length = 3)
    private String currency;

    @Embedded
    private ShippingAddress shippingAddress;

    // Unpaid orders are cancelled after this time and the diamonds go back on sale
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancel_reason", length = 30)
    private String cancelReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    @OrderBy("lineNumber")
    private List<OrderItem> items = new ArrayList<>();

    protected CustomerOrder() {
    }

    CustomerOrder(String orderNumber, UUID userId, ShippingAddress shippingAddress, Instant expiresAt) {
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.shippingAddress = shippingAddress;
        this.expiresAt = expiresAt;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.currency = CURRENCY;
    }

    void addItem(UUID productId, UUID variantId, String productSlug, String productName,
                 String sku, String description, String imageUrl, long pricePaise) {
        items.add(new OrderItem(items.size() + 1, productId, variantId, productSlug, productName,
                sku, description, imageUrl, pricePaise));
        subtotalPaise += pricePaise;
        recalculateTotal();
    }

    // Must match the database rule orders_total_chk
    private void recalculateTotal() {
        totalPaise = subtotalPaise + shippingPaise + gstPaise - discountPaise;
    }

    // Only an unpaid order can be cancelled this way
    void cancel(String reason, Instant now) {
        if (status != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Only unpaid orders can be cancelled");
        }
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = now;
        this.cancelReason = reason;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getOrderNumber() { return orderNumber; }
    public UUID getUserId() { return userId; }
    public OrderStatus getStatus() { return status; }
    public long getSubtotalPaise() { return subtotalPaise; }
    public long getShippingPaise() { return shippingPaise; }
    public long getDiscountPaise() { return discountPaise; }
    public long getGstPaise() { return gstPaise; }
    public long getTotalPaise() { return totalPaise; }
    public String getCurrency() { return currency; }
    public ShippingAddress getShippingAddress() { return shippingAddress; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getPaidAt() { return paidAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public String getCancelReason() { return cancelReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }
}