package com.lab.approval.service;

import com.lab.approval.ApprovalTask;
import com.lab.approval.controller.InternalApprovalController;
import jakarta.servlet.http.HttpServletRequest;

public interface ApprovalInternalService {
    ApprovalTask create(InternalApprovalController.CreateTask request, HttpServletRequest servletRequest);
    ApprovalTask createNext(ApprovalTask previous, java.time.LocalDateTime deadline);
    ApprovalTask closePending(Long bookingId, String reason, HttpServletRequest servletRequest);
    ApprovalTask reopenCanceled(Long bookingId, java.time.LocalDateTime deadline, HttpServletRequest servletRequest);
}
