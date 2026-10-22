package com.keianluxandor.backend.user;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.keianluxandor.backend.common.ForbiddenException;

// The "email must be verified" rule in one place. Checkout will call this before taking payment.
@Component
public class EmailVerificationGuard {

    public static final String EMAIL_NOT_VERIFIED = "EMAIL_NOT_VERIFIED";

    private final UserRepository userRepository;

    public EmailVerificationGuard(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Always reads fresh from the database, so a just-verified customer passes straight away
    @Transactional(readOnly = true)
    public void requireVerifiedEmail(UUID userId) {
        boolean verified = userRepository.findById(userId)
                .map(User::isEmailVerified)
                .orElse(false);

        if (!verified) {
            throw new ForbiddenException(EMAIL_NOT_VERIFIED,
                    "Please verify your email address before placing an order.");
        }
    }
}