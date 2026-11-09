package com.lab.user;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;
import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends CrudMapper<RefreshToken> {
    default Optional<RefreshToken> findByTokenHashAndRevokedAtIsNullAndExpiresAtAfterForUpdate(String tokenHash,Instant now) {
        return Optional.ofNullable(selectOne(Wrappers.<RefreshToken>query()
                .eq("token_hash", tokenHash).isNull("revoked_at").gt("expires_at", now).last("FOR UPDATE")));
    }
    default Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return Optional.ofNullable(selectOne(Wrappers.<RefreshToken>query().eq("token_hash", tokenHash)));
    }
    default long deleteByUserId(Long userId) {
        return delete(Wrappers.<RefreshToken>query().eq("user_id", userId));
    }
    default long deleteExpiredOrRevokedBefore(Instant cutoff) {
        return delete(Wrappers.<RefreshToken>query()
                .lt("expires_at", cutoff)
                .or()
                .isNotNull("revoked_at").lt("revoked_at", cutoff));
    }
}
