package com.lab.system.scheduler;
import com.lab.system.repository.OperationLogRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class OperationLogCleanupScheduler {
    private static final Logger log = LoggerFactory.getLogger(OperationLogCleanupScheduler.class);
    private final OperationLogRepository logs;
    private final int retentionDays;

    public OperationLogCleanupScheduler(OperationLogRepository logs,
                                        @Value("${system.operation-log.retention-days:90}") int retentionDays) {
        this.logs = logs;
        this.retentionDays = Math.max(1, retentionDays);
    }

    @Scheduled(cron = "0 30 3 * * *")
    public void cleanup() {
        long removed = logs.deleteByCreatedAtBefore(LocalDateTime.now().minusDays(retentionDays));
        if (removed > 0) log.info("Removed {} operation logs older than {} days", removed, retentionDays);
    }
}
