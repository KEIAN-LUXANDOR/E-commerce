package com.keianluxandor.backend.auth;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.keianluxandor.backend.common.ConflictException;
import com.keianluxandor.backend.common.TooManyRequestsException;
import com.keianluxandor.backend.common.UnauthorizedException;
import com.keianluxandor.backend.user.User;
import com.keianluxandor.backend.user.UserRepository;
import com.keianluxandor.backend.user.UserStatus;

@Service
public class AuthService {

    private static final String EMAIL_TAKEN = "An account with this email already exists.";
    private static final String INVALID_LOGIN = "Invalid email or password.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttempts;

    // Used when the email doesn't exist, so a wrong email takes as long as a wrong password.
    // This stops attackers from guessing which emails are registered by timing the response.
    private final String dummyHash;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       LoginAttemptService loginAttempts) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAttempts = loginAttempts;
        this.dummyHash = passwordEncoder.encode("not-a-real-password-just-for-timing");
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        // BCrypt only uses the first 72 BYTES. Emojis and some symbols use several bytes each.
        if (tooLongForBcrypt(request.password())) {
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

    // Checks email and password. Returns the user if correct.
    @Transactional(readOnly = true)
    public User login(LoginRequest request) {
        String email = request.email();

        if (loginAttempts.isLocked(email)) {
            throw new TooManyRequestsException(
                    "Too many failed login attempts. Please try again in 15 minutes.");
        }

        Optional<User> found = userRepository.findByEmailIgnoreCase(email);
        String hash = found.map(User::getPasswordHash).orElse(dummyHash);

        boolean passwordOk = !tooLongForBcrypt(request.password())
                && passwordEncoder.matches(request.password(), hash);

        if (found.isEmpty() || !passwordOk) {
            loginAttempts.recordFailure(email);
            throw new UnauthorizedException(INVALID_LOGIN);
        }

        User user = found.get();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("This account is disabled. Please contact support.");
        }

        loginAttempts.recordSuccess(email);
        return user;
    }

    // Who is logged in right now. Always read fresh from the database.
    @Transactional(readOnly = true)
    public UserResponse currentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new UnauthorizedException("Please log in again."));
        return UserResponse.from(user);
    }

    private boolean tooLongForBcrypt(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length > 72;
    }
}