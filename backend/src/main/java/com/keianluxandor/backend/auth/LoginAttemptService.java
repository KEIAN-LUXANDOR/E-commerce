package com.keianluxandor.backend.auth;

import java.util.Locale;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

// Locks an email for 15 minutes after 5 wrong passwords.
// Stored in the database (table login_attempts), so a lock survives restarts and works with several servers.
@Component
public class LoginAttemptService {

    private static final int MAX_FAILURES = 5;
    private static final String LOCK_TIME = "interval '15 minutes'";

    private final JdbcTemplate jdbc;

    // Each change runs in its own small transaction: if the login request itself fails and is rolled back,
    // the wrong-password count is still saved.
    private final TransactionTemplate ownTransaction;

    public LoginAttemptService(JdbcTemplate jdbc, PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.ownTransaction = new TransactionTemplate(transactionManager);
        this.ownTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public boolean isLocked(String email) {
        Boolean locked = jdbc.query(
                "SELECT failures >= ? AND first_failure > now() - " + LOCK_TIME
                        + " FROM login_attempts WHERE email = ?",
                rs -> rs.next() ? rs.getBoolean(1) : Boolean.FALSE,
                MAX_FAILURES, key(email));
        return Boolean.TRUE.equals(locked);
    }

    // Adds one wrong password. If the last count is older than 15 minutes, it starts again at 1.
    // One SQL statement, so two wrong passwords at the same moment are both counted.
    public void recordFailure(String email) {
        ownTransaction.executeWithoutResult(status -> jdbc.update("""
                INSERT INTO login_attempts (email, failures, first_failure) VALUES (?, 1, now())
                ON CONFLICT (email) DO UPDATE SET
                    failures = CASE WHEN login_attempts.first_failure <= now() - %1$s
                                    THEN 1 ELSE login_attempts.failures + 1 END,
                    first_failure = CASE WHEN login_attempts.first_failure <= now() - %1$s
                                         THEN now() ELSE login_attempts.first_failure END
                """.formatted(LOCK_TIME), key(email)));
    }

    // Correct password (or password reset): forget the wrong attempts
    public void recordSuccess(String email) {
        ownTransaction.executeWithoutResult(status ->
                jdbc.update("DELETE FROM login_attempts WHERE email = ?", key(email)));
    }

    // Once an hour, delete counts older than a day so the table stays small
    @Scheduled(fixedDelay = 3_600_000, initialDelay = 300_000)
    public void cleanUp() {
        jdbc.update("DELETE FROM login_attempts WHERE first_failure < now() - interval '1 day'");
    }

    private static String key(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}