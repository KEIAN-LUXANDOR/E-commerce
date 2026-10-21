package com.keianluxandor.backend.address;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AddressRepository extends JpaRepository<Address, UUID> {

    // Default first, then newest first
    List<Address> findByUserIdOrderByDefaultAddressDescCreatedAtDesc(UUID userId);

    // Always look up by id AND owner, so customers can't touch each other's addresses
    Optional<Address> findByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);

    Optional<Address> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

    // Runs straight away, so the "one default per customer" rule is never broken mid-change
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Address a set a.defaultAddress = false "
            + "where a.userId = :userId and a.defaultAddress = true")
    int clearDefault(@Param("userId") UUID userId);
}