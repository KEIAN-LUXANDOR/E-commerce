package com.keianluxandor.backend.payment;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// A webhook event we already processed. Rows are added by WebhookEventRepository.recordIfNew.
@Entity
@Table(name = "webhook_events")
public class WebhookEvent {

    @Id
    @Column(name = "event_id", length = 100)
    private String eventId;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected WebhookEvent() {
    }

    public String getEventId() { return eventId; }
    public String getProvider() { return provider; }
    public String getEventType() { return eventType; }
    public Instant getReceivedAt() { return receivedAt; }
}