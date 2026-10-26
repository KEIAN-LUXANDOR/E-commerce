package com.keianluxandor.backend.order;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface OrderRepository extends JpaRepository<CustomerOrder, UUID> {

    @Query(value = "SELECT nextval('order_number_seq')", nativeQuery = true)
    long nextOrderNumber();

    Optional<CustomerOrder> findByOrderNumberAndUserId(String orderNumber, UUID userId);

    Page<CustomerOrder> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<CustomerOrder> findFirstByUserIdAndStatus(UUID userId, OrderStatus status);

    // Locks the order row, so a payment and a cancellation can't change it at the same moment
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM CustomerOrder o WHERE o.id = :id")
    Optional<CustomerOrder> findByIdForUpdate(@Param("id") UUID id);

    // Same, found by order number, and only if it belongs to this customer
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM CustomerOrder o WHERE o.orderNumber = :orderNumber AND o.userId = :userId")
    Optional<CustomerOrder> findByOrderNumberAndUserIdForUpdate(@Param("orderNumber") String orderNumber,
                                                                @Param("userId") UUID userId);

    @Query("SELECT o.id FROM CustomerOrder o WHERE o.status = :status AND o.expiresAt < :now")
    List<UUID> findIdsExpiredBefore(@Param("status") OrderStatus status, @Param("now") Instant now);
}