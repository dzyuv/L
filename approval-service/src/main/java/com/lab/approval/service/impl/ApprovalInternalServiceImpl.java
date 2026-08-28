package com.lab.approval.service.impl;

import com.lab.approval.*;
import com.lab.approval.controller.InternalApprovalController;
import com.lab.approval.service.ApprovalInternalService;
import com.lab.common.api.InternalServiceGuard;
import com.lab.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class ApprovalInternalServiceImpl implements ApprovalInternalService {
    private final ApprovalTaskRepository tasks;
    private final InternalServiceGuard internalServices;

    public ApprovalInternalServiceImpl(ApprovalTaskRepository tasks, InternalServiceGuard internalServices) {
        this.tasks = tasks;
        this.internalServices = internalServices;
    }

    @Override
    @Transactional
    public ApprovalTask create(InternalApprovalController.CreateTask request, HttpServletRequest servletRequest) {
        internalServices.require(servletRequest);
        ApprovalTask task = tasks.findByBookingId(request.bookingId()).orElseGet(ApprovalTask::new);
        if (task.id != null) {
            if (!"PENDING".equals(task.status)) throw new BusinessException("TASK_ALREADY_COMPLETED", "Approval task has already been processed", HttpStatus.CONFLICT);
            return task;
        }
        task.bookingId = request.bookingId();
        task.applicantUserId = request.applicantUserId();
        task.applicantName = request.applicantName();
        task.resourceId = request.resourceId();
        task.resourceName = request.resourceName();
        task.startTime = request.startTime();
        task.endTime = request.endTime();
        task.level = request.level();
        task.approverRole = "RESOURCE_MANAGER";
        task.assignedUserId = request.assignedUserId();
        LocalDateTime defaultDeadline = LocalDateTime.now().plusHours(24);
        task.deadline = request.startTime() != null && request.startTime().isBefore(defaultDeadline)
                ? request.startTime() : defaultDeadline;
        return tasks.save(task);
    }
}
