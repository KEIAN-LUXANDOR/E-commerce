package com.keianluxandor.backend.auth;

import java.nio.charset.StandardCharsets;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.keianluxandor.backend.common.ConflictException;
import com.keianluxandor.backend.user.User;
import com.keianluxandor.backend.user.UserRepository;

@Service
public class AuthService {

    private static final String EMAIL_TAKEN = "An account with this email already exists.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        // BCrypt only uses the first 72 BYTES. Emojis and some symbols use several bytes each.
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password is too long. Use fewer characters or fewer special symbols.");
        }

        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException(EMAIL_TAKEN);
        }

        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.fullName(),
                request.phone());

        try {
            return UserResponse.from(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException e) {
            // Two people registered the same email at the same moment; the database blocked one
            throw new ConflictException(EMAIL_TAKEN);
        }
    }
}