package com.keianluxandor.backend.payment;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

interface WebhookEventRepository extends Repository<WebhookEvent, String> {

    // Returns 1 the first time an event id is seen, 0 if it was already processed
    @Modifying
    @Query(value = """
            INSERT INTO webhook_events (event_id, provider, event_type, received_at)
            VALUES (:eventId, 'RAZORPAY', :eventType, now())
            ON CONFLICT (event_id) DO NOTHING
            """, nativeQuery = true)
    int recordIfNew(@Param("eventId") String eventId, @Param("eventType") String eventType);
}