package com.lab.user.scheduler;
import com.lab.user.entity.*;
import com.lab.user.repository.*;
import com.lab.user.client.*;
import com.lab.user.seed.*;
import com.lab.user.security.*;
import com.lab.user.config.*;
import com.lab.user.controller.*;
import com.lab.user.service.*;
import com.lab.user.service.impl.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class RefreshTokenCleanupScheduler {
    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupScheduler.class);
    private final RefreshTokenRepository tokens;
    private final int retentionDays;

    public RefreshTokenCleanupScheduler(RefreshTokenRepository tokens,
                                        @Value("${security.refresh-token.cleanup-retention-days:7}") int retentionDays) {
        this.tokens = tokens;
        this.retentionDays = Math.max(1, retentionDays);
    }

    @Scheduled(cron = "0 20 3 * * *")
    public void cleanup() {
        long removed = tokens.deleteExpiredOrRevokedBefore(Instant.now().minusSeconds(retentionDays * 86_400L));
        if (removed > 0) log.info("Removed {} expired or revoked refresh tokens", removed);
    }
}
