package com.keianluxandor.backend.contact;

import java.time.Instant;
import java.util.UUID;

// A contact message as the admin sees it
public record ContactMessageResponse(
        UUID id,
        String name,
        String email,
        String phone,
        String subject,
        String orderNumber,
        String message,
        ContactMessage.Status status,
        boolean fromLoggedInCustomer,
        Instant createdAt,
        Instant handledAt
) {
    static ContactMessageResponse from(ContactMessage m) {
        return new ContactMessageResponse(
                m.getId(), m.getName(), m.getEmail(), m.getPhone(), m.getSubject(), m.getOrderNumber(),
                m.getMessage(), m.getStatus(), m.getUserId() != null, m.getCreatedAt(), m.getHandledAt());
    }
}