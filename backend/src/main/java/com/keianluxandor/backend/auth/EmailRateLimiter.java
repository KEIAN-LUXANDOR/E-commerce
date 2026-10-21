package com.keianluxandor.backend.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

// At most one "resend" or "forgot password" email per address per minute.
// Kept in memory: resets when the backend restarts. Move to the database before going live.
@Component
public class EmailRateLimiter {

    private static final Duration WAIT = Duration.ofSeconds(60);

    private final Map<String, Instant> lastSent = new ConcurrentHashMap<>();

    public boolean tryAcquire(String key) {
        Instant now = Instant.now();
        boolean[] allowed = {false};
        lastSent.compute(key, (k, last) -> {
            if (last == null || now.isAfter(last.plus(WAIT))) {
                allowed[0] = true;
                return now;
            }
            return last;
        });
        return allowed[0];
    }
}