package com.keianluxandor.backend.order;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

// Copy of the delivery address at the time of ordering, so later edits to the
// customer's saved address don't change an order that was already placed.
@Embeddable
public record ShippingAddress(
        @Column(name = "ship_full_name", nullable = false, length = 100) String fullName,
        @Column(name = "ship_phone", nullable = false, length = 16) String phone,       // e.g. +14155550100
        @Column(name = "ship_line1", nullable = false, length = 200) String line1,
        @Column(name = "ship_line2", length = 200) String line2,
        @Column(name = "ship_landmark", length = 100) String landmark,
        @Column(name = "ship_city", nullable = false, length = 80) String city,
        @Column(name = "ship_state", length = 100) String state,
        @Column(name = "ship_pincode", length = 10) String pincode,                     // postal code / ZIP
        @Column(name = "ship_country", nullable = false, length = 2) String country     // e.g. US
) {
}