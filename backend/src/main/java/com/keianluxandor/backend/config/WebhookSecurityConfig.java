package com.keianluxandor.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

// Security for the Razorpay webhook ONLY.
// Razorpay's servers have no login session or CSRF token; every call is checked with its
// HMAC signature in RazorpayWebhookController instead. All other URLs use SecurityConfig.
@Configuration
public class WebhookSecurityConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    SecurityFilterChain razorpayWebhookSecurity(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/v1/payments/razorpay/webhook")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }
}