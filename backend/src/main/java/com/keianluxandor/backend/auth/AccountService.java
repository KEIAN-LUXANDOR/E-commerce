package com.keianluxandor.backend.auth;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import com.keianluxandor.backend.mail.EmailService;
import com.keianluxandor.backend.user.TokenType;
import com.keianluxandor.backend.user.User;
import com.keianluxandor.backend.user.UserRepository;
import com.keianluxandor.backend.user.UserStatus;

@Service
public class AccountService {

    private static final Duration VERIFY_LINK_VALID = Duration.ofHours(24);
    private static final Duration RESET_LINK_VALID = Duration.ofMinutes(30);
    private static final String INVALID_LINK = "This link is invalid or has expired. Please request a new one.";

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final EmailRateLimiter rateLimiter;
    private final LoginAttemptService loginAttempts;
    private final String frontendUrl;

    public AccountService(UserRepository userRepository,
                          TokenService tokenService,
                          EmailService emailService,
                          PasswordEncoder passwordEncoder,
                          EmailRateLimiter rateLimiter,
                          LoginAttemptService loginAttempts,
                          @Value("${app.frontend-url}") String frontendUrl) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiter = rateLimiter;
        this.loginAttempts = loginAttempts;
        this.frontendUrl = frontendUrl;
    }

    // Called right after registration, and by "resend"
    @Transactional
    public void sendVerificationEmail(User user) {
        String token = tokenService.create(user, TokenType.EMAIL_VERIFICATION, VERIFY_LINK_VALID);
        String link = frontendUrl + "/verify-email?token=" + token;

        sendAfterSave(user.getEmail(), "Verify your email address",
                "Hello " + user.getFullName() + ",\n\n"
                + "Please confirm your email address by opening this link:\n"
                + link + "\n\n"
                + "The link is valid for 24 hours.\n"
                + "If you did not create an account, you can ignore this email.");
    }

    // Always looks the same to the caller, whether or not the email exists
    @Transactional
    public void resendVerification(String email) {
        if (!rateLimiter.tryAcquire("verify:" + email)) {
            return;
        }
        userRepository.findByEmailIgnoreCase(email)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE && !user.isEmailVerified())
                .ifPresent(this::sendVerificationEmail);
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = tokenService.consume(token, TokenType.EMAIL_VERIFICATION)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_LINK));
        user.markEmailVerified();
    }
    // Always looks the same to the caller, whether or not the email exists
    @Transactional
    public void requestPasswordReset(String email) {
        if (!rateLimiter.tryAcquire("reset:" + email)) {
            return;
        }
        userRepository.findByEmailIgnoreCase(email)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .ifPresent(user -> {
                    String token = tokenService.create(user, TokenType.PASSWORD_RESET, RESET_LINK_VALID);
                    String link = frontendUrl + "/reset-password?token=" + token;

                    sendAfterSave(user.getEmail(), "Reset your password",
                            "Hello " + user.getFullName() + ",\n\n"
                            + "We received a request to reset your password. Open this link to choose a new one:\n"
                            + link + "\n\n"
                            + "The link is valid for 30 minutes and can be used once.\n"
                            + "If you did not ask for this, you can ignore this email. Your password will not change.");
                });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password is too long. Use fewer characters or fewer special symbols.");
        }

        User user = tokenService.consume(token, TokenType.PASSWORD_RESET)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_LINK));

        user.changePassword(passwordEncoder.encode(newPassword));

        // Opening the email link proves they own this email address
        if (!user.isEmailVerified()) {
            user.markEmailVerified();
        }

        // Clear any login lock from earlier wrong passwords
        loginAttempts.recordSuccess(user.getEmail());
    }

    // Send only after the database save succeeds, so we never email a code that wasn't saved
    private void sendAfterSave(String to, String subject, String text) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emailService.send(to, subject, text);
                }
            });
        } else {
            emailService.send(to, subject, text);
        }
    }
}