package com.lab.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken,Long> {
    Optional<RefreshToken> findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(String tokenHash,Instant now);
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    long deleteByUserId(Long userId);
}
