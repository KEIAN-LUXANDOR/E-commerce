package com.keianluxandor.backend.auth;

import java.time.Instant;
import java.util.UUID;

import com.keianluxandor.backend.user.Role;
import com.keianluxandor.backend.user.User;

// Never contains the password or its hash
public record UserResponse(
        UUID id,
        String email,
        String fullName,
        String phone,
        Role role,
        boolean emailVerified,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole(),
                user.isEmailVerified(),
                user.getCreatedAt()
        );
    }
}