package com.keianluxandor.backend.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByEmailIgnoreCase(String email);

    // Used for login in Part B
    Optional<User> findByEmailIgnoreCase(String email);
}