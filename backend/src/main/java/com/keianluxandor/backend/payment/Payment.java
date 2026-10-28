package com.keianluxandor.backend.payment;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

// One payment attempt for an order (Razorpay needs a new Razorpay order for every attempt)
@Entity
@Table(name = "payments")
public class Payment {

    public static final String PROVIDER_RAZORPAY = "RAZORPAY";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "attempt_number", nullable = false, updatable = false)
    private int attemptNumber;

    @Column(nullable = false, length = 20, updatable = false)
    private String provider;

    // Razorpay order id, e.g. "order_Pa1b2C3d4E5f6G"
    @Column(name = "provider_order_id", nullable = false, unique = true, length = 50, updatable = false)
    private String providerOrderId;

    // Razorpay payment id, e.g. "pay_Pa1b2C3d4E5f6G" (set when paid)
    @Column(name = "provider_payment_id", unique = true, length = 50)
    private String providerPaymentId;

    @Column(name = "amount_paise", nullable = false, updatable = false)
    private long amountPaise;

    @Column(nullable = false, length = 3, updatable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Payment() {
    }

    Payment(UUID orderId, int attemptNumber, String providerOrderId, long amountPaise, String currency) {
        this.orderId = orderId;
        this.attemptNumber = attemptNumber;
        this.provider = PROVIDER_RAZORPAY;
        this.providerOrderId = providerOrderId;
        this.amountPaise = amountPaise;
        this.currency = currency;
        this.status = PaymentStatus.CREATED;
    }

    void markPaid(String providerPaymentId) {
        this.providerPaymentId = providerPaymentId;
        this.status = PaymentStatus.PAID;
    }

    void markRefundRequired(String providerPaymentId) {
        this.providerPaymentId = providerPaymentId;
        this.status = PaymentStatus.REFUND_REQUIRED;
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
    public UUID getOrderId() { return orderId; }
    public int getAttemptNumber() { return attemptNumber; }
    public String getProvider() { return provider; }
    public String getProviderOrderId() { return providerOrderId; }
    public String getProviderPaymentId() { return providerPaymentId; }
    public long getAmountPaise() { return amountPaise; }
    public String getCurrency() { return currency; }
    public PaymentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}