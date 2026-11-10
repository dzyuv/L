package com.lab.booking.approval.service;
import com.lab.booking.entity.*;
import com.lab.booking.repository.*;
import com.lab.booking.client.*;
import com.lab.booking.scheduler.*;
import com.lab.booking.config.*;
import com.lab.booking.service.*;
import com.lab.booking.service.impl.*;
import com.lab.booking.controller.*;
import com.lab.booking.approval.entity.*;
import com.lab.booking.approval.repository.*;
import com.lab.booking.approval.service.impl.*;
import com.lab.booking.approval.controller.*;

import com.lab.booking.approval.entity.ApprovalTask;

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

    int releaseAssignedTo(Long userId);
}
