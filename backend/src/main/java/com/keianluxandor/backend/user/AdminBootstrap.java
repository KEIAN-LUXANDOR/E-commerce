package com.keianluxandor.backend.user;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Creates the first ADMIN when the backend starts, if ADMIN_EMAIL and ADMIN_PASSWORD are set.
// Does nothing if the account already exists. Never logs the password.
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminBootstrap(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${app.admin.email:}") String adminEmail,
                          @Value("${app.admin.password:}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.info("ADMIN_EMAIL / ADMIN_PASSWORD not set: skipping admin setup.");
            return;
        }

        String email = adminEmail.trim().toLowerCase(Locale.ROOT);

        if (adminPassword.length() < 12 || adminPassword.length() > 72) {
            log.warn("ADMIN_PASSWORD must be 12 to 72 characters: admin not created.");
            return;
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            log.info("An account with the admin email already exists: {} (not changed).", email);
            return;
        }

        userRepository.save(User.newAdmin(email, passwordEncoder.encode(adminPassword), "Store Admin"));
        log.info("Created admin account: {}", email);
    }
}