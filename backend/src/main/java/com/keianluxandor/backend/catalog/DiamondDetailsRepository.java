package com.keianluxandor.backend.catalog;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DiamondDetailsRepository extends JpaRepository<DiamondDetails, UUID> {
}