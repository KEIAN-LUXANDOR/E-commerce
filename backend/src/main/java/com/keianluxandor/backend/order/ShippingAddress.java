package com.keianluxandor.backend.order;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

// Copy of the delivery address saved inside the order.
// Editing or deleting the address later does not change old orders.
@Embeddable
public record ShippingAddress(
        @Column(name = "ship_full_name", nullable = false, length = 100) String fullName,
        @Column(name = "ship_phone", nullable = false, length = 10) String phone,
        @Column(name = "ship_line1", nullable = false, length = 200) String line1,
        @Column(name = "ship_line2", length = 200) String line2,
        @Column(name = "ship_landmark", length = 100) String landmark,
        @Column(name = "ship_city", nullable = false, length = 80) String city,
        @Column(name = "ship_state", nullable = false, length = 60) String state,
        @Column(name = "ship_pincode", nullable = false, length = 6) String pincode) {
}