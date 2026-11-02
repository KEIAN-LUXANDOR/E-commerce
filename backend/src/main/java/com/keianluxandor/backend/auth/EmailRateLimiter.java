package com.keianluxandor.backend.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// At most one "resend verification" or "forgot password" email per address per minute.
// Stored in the database (table rate_limits), so it survives restarts and works with several servers.
@Component
public class EmailRateLimiter {

    private final JdbcTemplate jdbc;

    public EmailRateLimiter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // true = allowed (and the time is recorded); false = an email was already sent in the last 60 seconds.
    // One SQL statement, so two requests at the same moment can't both get through.
    public boolean tryAcquire(String key) {
        int rows = jdbc.update("""
                INSERT INTO rate_limits (limit_key, last_at) VALUES (?, now())
                ON CONFLICT (limit_key) DO UPDATE SET last_at = now()
                WHERE rate_limits.last_at < now() - interval '60 seconds'
                """, key);
        return rows == 1;
    }

    // Once an hour, delete entries older than a day so the table stays small
    @Scheduled(fixedDelay = 3_600_000, initialDelay = 300_000)
    public void cleanUp() {
        jdbc.update("DELETE FROM rate_limits WHERE last_at < now() - interval '1 day'");
    }
}