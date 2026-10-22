package com.keianluxandor.backend.profile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import com.keianluxandor.backend.auth.LoginAttemptService;
import com.keianluxandor.backend.auth.UserResponse;
import com.keianluxandor.backend.common.TooManyRequestsException;
import com.keianluxandor.backend.common.UnauthorizedException;
import com.keianluxandor.backend.mail.EmailService;
import com.keianluxandor.backend.user.User;
import com.keianluxandor.backend.user.UserRepository;
import com.keianluxandor.backend.user.UserStatus;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttempts;
    private final EmailService emailService;

    public ProfileService(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          LoginAttemptService loginAttempts,
                          EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAttempts = loginAttempts;
        this.emailService = emailService;
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = activeUser(userId);
        user.updateProfile(request.fullName(), request.phone());
        return UserResponse.from(userRepository.saveAndFlush(user));
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = activeUser(userId);
        String email = user.getEmail();

        // Wrong current passwords count toward the same lock as login
        if (loginAttempts.isLocked(email)) {
            throw new TooManyRequestsException("Too many wrong passwords. Please try again in 15 minutes.");
        }

        boolean currentOk = !tooLongForBcrypt(request.currentPassword())
                && passwordEncoder.matches(request.currentPassword(), user.getPasswordHash());
        if (!currentOk) {
            loginAttempts.recordFailure(email);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your current password is incorrect.");
        }

        if (tooLongForBcrypt(request.newPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password is too long. Use fewer characters or fewer special symbols.");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Your new password must be different from your current password.");
        }

        user.changePassword(passwordEncoder.encode(request.newPassword()));
        loginAttempts.recordSuccess(email);

        // Tell the owner, in case it wasn't them
        sendAfterSave(email, "Your password was changed",
                "Hello " + user.getFullName() + ",\n\n"
                + "The password for your account was just changed.\n\n"
                + "If this was you, no action is needed.\n"
                + "If this was not you, use \"Forgot password\" on the login page straight away "
                + "and contact our support team.");
    }

    private User activeUser(UUID userId) {
        return userRepository.findById(userId)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new UnauthorizedException("Please log in again."));
    }

    private boolean tooLongForBcrypt(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length > 72;
    }

    // Send only after the database save succeeds
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