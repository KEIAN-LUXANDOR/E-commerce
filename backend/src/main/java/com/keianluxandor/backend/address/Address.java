package com.keianluxandor.backend.address;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    // With country code, e.g. +14155550100
    @Column(nullable = false, length = 16)
    private String phone;

    @Column(nullable = false, length = 200)
    private String line1;

    @Column(length = 200)
    private String line2;

    @Column(length = 100)
    private String landmark;

    @Column(nullable = false, length = 80)
    private String city;

    // State / province / region (empty for many countries)
    @Column(length = 100)
    private String state;

    // Postal code / ZIP / PIN code (empty for countries without one)
    @Column(length = 10)
    private String pincode;

    // 2-letter country code: US, GB, AE, IN ...
    @Column(nullable = false, length = 2)
    private String country;

    @Column(name = "is_default", nullable = false)
    private boolean defaultAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Address() {
    }

    public Address(UUID userId) {
        this.userId = userId;
    }

    // Copies the fields from the request. Country, phone, state and postal code are already checked.
    void apply(AddressRequest request, String country, String phone, String state, String pincode) {
        this.fullName = request.fullName();
        this.phone = phone;
        this.line1 = request.line1();
        this.line2 = request.line2();
        this.landmark = request.landmark();
        this.city = request.city();
        this.state = state;
        this.pincode = pincode;
        this.country = country;
    }

    void setDefaultAddress(boolean defaultAddress) {
        this.defaultAddress = defaultAddress;
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

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getLine1() {
        return line1;
    }

    public String getLine2() {
        return line2;
    }

    public String getLandmark() {
        return landmark;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getPincode() {
        return pincode;
    }

    public String getCountry() {
        return country;
    }

    public boolean isDefaultAddress() {
        return defaultAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}