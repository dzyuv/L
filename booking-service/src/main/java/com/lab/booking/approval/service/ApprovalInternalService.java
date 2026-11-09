package com.lab.booking.approval.service;

import com.lab.booking.approval.ApprovalTask;

import java.time.LocalDateTime;

public interface ApprovalInternalService {
    record CreateTask(Long bookingId, Long applicantUserId, String applicantName, Long resourceId,
                      Long resourceTypeId, String resourceName, LocalDateTime startTime, LocalDateTime endTime,
                      int level, int totalLevels, Long assignedUserId, String approverRole) {}

    ApprovalTask create(CreateTask request);

    ApprovalTask createNext(ApprovalTask previous, LocalDateTime deadline);

    ApprovalTask closePending(Long bookingId, String reason);

    ApprovalTask closePendingInNewTransaction(Long bookingId, String reason);

    ApprovalTask reopenCanceled(Long bookingId, LocalDateTime deadline);
}
