package com.keianluxandor.backend.auth;

import java.io.Serializable;
import java.util.UUID;

import com.keianluxandor.backend.user.Role;

// Who is logged in. Stored in the session, so it must be Serializable.
public record AuthUser(UUID id, String email, Role role) implements Serializable {
}