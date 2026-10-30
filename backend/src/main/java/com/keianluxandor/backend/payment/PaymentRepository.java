package com.keianluxandor.backend.payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    long countByOrderId(UUID orderId);

    boolean existsByOrderIdAndStatusAndIdNot(UUID orderId, PaymentStatus status, UUID id);

    // Admin: all payment attempts of one order
    List<Payment> findByOrderIdOrderByAttemptNumberAsc(UUID orderId);

    // Admin: e.g. how many payments must be refunded
    long countByStatus(PaymentStatus status);

    // Locks the payment row, so the checkout callback and the webhook can't record it twice
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.providerOrderId = :providerOrderId")
    Optional<Payment> findByProviderOrderIdForUpdate(@Param("providerOrderId") String providerOrderId);
}