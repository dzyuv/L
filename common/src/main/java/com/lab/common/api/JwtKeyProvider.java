package com.lab.common.api;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/** Provides the signing key from deployment configuration rather than source code. */
@Component
public class JwtKeyProvider {
    private final SecretKey key;

    public JwtKeyProvider(@Value("${security.jwt-secret:}") String secret) {
        if (secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 characters");
        }
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public SecretKey key() {
        return key;
    }
}
