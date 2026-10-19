package com.keianluxandor.backend.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

// Locks an email for 15 minutes after 5 wrong passwords.
// Kept in memory: it resets when the backend restarts. Move it to the database before going live.
@Component
public class LoginAttemptService {

    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_TIME = Duration.ofMinutes(15);

    private record Attempt(int failures, Instant firstFailure) {
    }

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    public boolean isLocked(String email) {
        Attempt attempt = attempts.get(email);
        if (attempt == null) {
            return false;
        }
        if (isExpired(attempt)) {
            attempts.remove(email);
            return false;
        }
        return attempt.failures() >= MAX_FAILURES;
    }

    public void recordFailure(String email) {
        attempts.compute(email, (key, attempt) ->
                (attempt == null || isExpired(attempt))
                        ? new Attempt(1, Instant.now())
                        : new Attempt(attempt.failures() + 1, attempt.firstFailure()));
    }

    public void recordSuccess(String email) {
        attempts.remove(email);
    }

    private boolean isExpired(Attempt attempt) {
        return Instant.now().isAfter(attempt.firstFailure().plus(LOCK_TIME));
    }
}