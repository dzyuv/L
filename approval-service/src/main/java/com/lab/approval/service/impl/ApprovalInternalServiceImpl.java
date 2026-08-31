package com.lab.approval.service.impl;

import com.lab.approval.*;
import com.lab.approval.controller.InternalApprovalController;
import com.lab.approval.service.ApprovalInternalService;
import com.lab.common.api.InternalServiceGuard;
import com.lab.common.api.Roles;
import com.lab.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApprovalInternalServiceImpl implements ApprovalInternalService {
    private final ApprovalTaskRepository tasks;
    private final ApprovalFlowRepository flows;
    private final ApprovalNodeRepository nodes;
    private final InternalServiceGuard internalServices;
    private final int timeoutHours;

    public ApprovalInternalServiceImpl(ApprovalTaskRepository tasks, ApprovalFlowRepository flows,
                                       ApprovalNodeRepository nodes, InternalServiceGuard internalServices,
                                       @Value("${approval.timeout-hours:24}") int timeoutHours) {
        this.tasks = tasks;
        this.flows = flows;
        this.nodes = nodes;
        this.internalServices = internalServices;
        this.timeoutHours = timeoutHours;
    }

    @Override
    @Transactional
    public ApprovalTask create(InternalApprovalController.CreateTask request, HttpServletRequest servletRequest) {
        internalServices.require(servletRequest);
        return upsert(request);
    }

    @Override
    @Transactional
    public ApprovalTask createNext(ApprovalTask previous) {
        if (previous == null || previous.totalLevels <= previous.level) return null;
        return upsert(new InternalApprovalController.CreateTask(
                previous.bookingId, previous.applicantUserId, previous.applicantName,
                previous.resourceId, previous.resourceTypeId, previous.resourceName,
                previous.startTime, previous.endTime,
                previous.level + 1, previous.totalLevels, null, Roles.LAB_ADMIN));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ApprovalTask closePending(Long bookingId, String reason, HttpServletRequest servletRequest) {
        if (servletRequest != null) internalServices.require(servletRequest);
        String status = normalizeCloseReason(reason);
        List<ApprovalTask> pending = tasks.findByBookingIdAndStatus(bookingId, "PENDING");
        ApprovalTask last = null;
        LocalDateTime now = LocalDateTime.now();
        String comment = "EXPIRED".equals(status) ? "Approval hold expired" : "Canceled by applicant";
        for (ApprovalTask task : pending) {
            task.status = status;
            task.completedAt = now;
            if (task.comment == null || task.comment.isBlank()) task.comment = comment;
            last = tasks.save(task);
        }
        return last;
    }

    private ApprovalTask upsert(InternalApprovalController.CreateTask request) {
        int level = Math.max(1, request.level());
        int totalLevels = Math.max(level, Math.max(request.totalLevels(), flowLevelCount(request.resourceTypeId())));
        ApprovalTask task = tasks.findByBookingIdAndLevel(request.bookingId(), level).orElseGet(ApprovalTask::new);
        if (task.id != null) {
            if (!"PENDING".equals(task.status)) {
                throw new BusinessException("TASK_ALREADY_COMPLETED", "Approval task has already been processed", HttpStatus.CONFLICT);
            }
            return task;
        }
        ApprovalNode node = nodeFor(request.resourceTypeId(), level);
        task.bookingId = request.bookingId();
        task.applicantUserId = request.applicantUserId();
        task.applicantName = request.applicantName();
        task.resourceId = request.resourceId();
        task.resourceTypeId = request.resourceTypeId();
        task.resourceName = request.resourceName();
        task.startTime = request.startTime();
        task.endTime = request.endTime();
        task.level = level;
        task.totalLevels = totalLevels;
        Long assigned = request.assignedUserId();
        String role = request.approverRole() == null ? "" : request.approverRole().trim();
        if (node != null && node.approverRole != null && !node.approverRole.isBlank()) {
            role = node.approverRole.trim();
        }
        if (role.isBlank()) {
            if (totalLevels >= 2 && level == 1) role = Roles.TEACHER;
            else role = assigned == null ? Roles.LAB_ADMIN : Roles.TEACHER;
        }
        if (Roles.TEACHER.equals(role)) {
            if (totalLevels >= 2) assigned = null;
        } else {
            assigned = null;
            role = Roles.LAB_ADMIN;
        }
        task.assignedUserId = assigned;
        task.approverRole = role;
        task.deadline = deadline(request.startTime(), node);
        return tasks.save(task);
    }

    private int flowLevelCount(Long resourceTypeId) {
        return flows.findEnabledByResourceTypeId(resourceTypeId)
                .map(flow -> nodes.findByFlowIdOrderByLevelAscSequenceNoAsc(flow.id).stream()
                        .mapToInt(item -> item.level).max().orElse(0))
                .orElse(0);
    }

    private ApprovalNode nodeFor(Long resourceTypeId, int level) {
        return flows.findEnabledByResourceTypeId(resourceTypeId)
                .map(flow -> nodes.findByFlowIdOrderByLevelAscSequenceNoAsc(flow.id).stream()
                        .filter(item -> item.level == level)
                        .findFirst().orElse(null))
                .orElse(null);
    }

    private LocalDateTime deadline(LocalDateTime startTime, ApprovalNode node) {
        int minutes = node != null && node.deadlineMinutes > 0 ? node.deadlineMinutes : timeoutHours * 60;
        LocalDateTime byTimeout = LocalDateTime.now().plusMinutes(minutes);
        if (startTime != null && startTime.isBefore(byTimeout)) return startTime;
        return byTimeout;
    }

    private String normalizeCloseReason(String reason) {
        String status = reason == null ? "" : reason.trim().toUpperCase();
        if (!"CANCELED".equals(status) && !"EXPIRED".equals(status)) {
            throw new BusinessException("INVALID_STATUS", "Close reason must be CANCELED or EXPIRED", HttpStatus.BAD_REQUEST);
        }
        return status;
    }
}
