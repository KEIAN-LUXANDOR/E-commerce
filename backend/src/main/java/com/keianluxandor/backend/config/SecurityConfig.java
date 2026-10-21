package com.keianluxandor.backend.config;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    // Anyone can call these (the person isn't logged in yet), and they don't need a CSRF token
    private static final String[] PUBLIC_AUTH_POSTS = {
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/verify-email",
            "/api/v1/auth/resend-verification",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            // CSRF token needed for every change, except the public account actions above
            .csrf(csrf -> csrf.ignoringRequestMatchers(PUBLIC_AUTH_POSTS))
            .authorizeHttpRequests(auth -> auth
                // Public: anyone can browse the catalog
                .requestMatchers(HttpMethod.GET, "/api/v1/products/**", "/api/v1/categories/**", "/images/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                // Public: account actions and the CSRF token
                .requestMatchers(HttpMethod.POST, PUBLIC_AUTH_POSTS).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf").permitAll()
                .requestMatchers("/error").permitAll()
                // Admin only: product management, all orders
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                // Everything else needs a logged-in user
                .anyRequest().authenticated())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, e) ->
                        writeProblem(response, 401, "Unauthorized", "Please log in to continue."))
                .accessDeniedHandler((request, response, e) -> {
                    if (e instanceof CsrfException) {
                        writeProblem(response, 403, "Forbidden", "Missing or invalid CSRF token.");
                    } else {
                        writeProblem(response, 403, "Forbidden", "You do not have permission to do this.");
                    }
                }));

        return http.build();
    }

    // Which websites may call this API from a browser
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:3000}") String[] allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "X-CSRF-TOKEN"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    // Same error format as ApiExceptionHandler, for errors raised before reaching a controller
    private static void writeProblem(HttpServletResponse response, int status, String title, String detail)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"status\":" + status + ",\"title\":\"" + title + "\",\"detail\":\"" + detail + "\"}");
    }
}