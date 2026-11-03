package com.keianluxandor.backend.contact;

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
import jakarta.persistence.Table;

// A message sent with the "Contact us" form
@Entity
@Table(name = "contact_messages")
public class ContactMessage {

    public enum Status { NEW, HANDLED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(length = 25)
    private String phone;

    @Column(length = 150)
    private String subject;

    @Column(name = "order_number", length = 20)
    private String orderNumber;

    @Column(nullable = false, length = 5000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "handled_at")
    private Instant handledAt;

    protected ContactMessage() {
    }

    ContactMessage(UUID userId, ContactRequest request) {
        this.userId = userId;
        this.name = request.name();
        this.email = request.email();
        this.phone = request.phone();
        this.subject = request.subject();
        this.orderNumber = request.orderNumber();
        this.message = request.message();
        this.status = Status.NEW;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    void markHandled() {
        if (status != Status.HANDLED) {
            status = Status.HANDLED;
            handledAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getSubject() { return subject; }
    public String getOrderNumber() { return orderNumber; }
    public String getMessage() { return message; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getHandledAt() { return handledAt; }
}