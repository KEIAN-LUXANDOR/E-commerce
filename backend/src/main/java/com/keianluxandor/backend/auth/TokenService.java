package com.keianluxandor.backend.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.keianluxandor.backend.user.TokenType;
import com.keianluxandor.backend.user.User;
import com.keianluxandor.backend.user.UserToken;
import com.keianluxandor.backend.user.UserTokenRepository;

@Service
public class TokenService {

    private final SecureRandom random = new SecureRandom();
    private final UserTokenRepository tokenRepository;

    public TokenService(UserTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    // Creates a new code, cancels older ones, saves only the hash, returns the real code
    @Transactional
    public String create(User user, TokenType type, Duration validFor) {
        tokenRepository.invalidateAll(user, type, Instant.now());

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        tokenRepository.save(new UserToken(user, type, sha256(rawToken), Instant.now().plus(validFor)));
        return rawToken;
    }

    // Checks the code. If valid, marks it used and returns its user.
    @Transactional
    public Optional<User> consume(String rawToken, TokenType type) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > 100) {
            return Optional.empty();
        }
        return tokenRepository.findByTokenHashAndType(sha256(rawToken), type)
                .filter(token -> token.isUsable(Instant.now()))
                .map(token -> {
                    token.markUsed();
                    return token.getUser();
                });
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}