package com.lab.approval.service.impl;

import com.lab.approval.*;
import com.lab.approval.service.ApprovalInternalService;
import com.lab.approval.service.ApprovalTaskService;
import com.lab.common.api.RoleGuard;
import com.lab.common.api.Roles;
import com.lab.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ApprovalTaskServiceImpl implements ApprovalTaskService {
    private static final Set<String> STALE_BOOKING_CODES = Set.of("BOOKING_CANCELED", "BOOKING_EXPIRED", "BOOKING_NOT_PENDING");
    private final ApprovalTaskRepository tasks;
    private final ApprovalRecordRepository records;
    private final BookingDecisionClient bookingDecisions;
    private final ApprovalInternalService internalApprovals;
    private final RoleGuard roleGuard;

    public ApprovalTaskServiceImpl(ApprovalTaskRepository tasks, ApprovalRecordRepository records,
                                   BookingDecisionClient bookingDecisions, ApprovalInternalService internalApprovals,
                                   RoleGuard roleGuard) {
        this.tasks = tasks;
        this.records = records;
        this.bookingDecisions = bookingDecisions;
        this.internalApprovals = internalApprovals;
        this.roleGuard = roleGuard;
    }

    @Override
    public List<ApprovalTask> mine(HttpServletRequest request) {
        requireApproverRole(request);
        Long userId = currentUser(request);
        List<ApprovalTask> items = new ArrayList<>();
        if (roleGuard.hasRole(request, Roles.TEACHER)) items.addAll(tasks.findPendingForTeacher(userId));
        if (roleGuard.hasRole(request, Roles.LAB_ADMIN)) items.addAll(tasks.findPendingForLabAdmin(userId));
        if (items.isEmpty()) items.addAll(tasks.findByAssignedUserIdAndStatus(userId, "PENDING"));
        Map<Long, ApprovalTask> unique = new LinkedHashMap<>();
        for (ApprovalTask task : items) unique.putIfAbsent(task.id, task);
        return unique.values().stream()
                .filter(task -> !Objects.equals(task.applicantUserId, userId))
                .filter(task -> !expired(task))
                .toList();
    }

    @Override
    @Transactional
    public ApprovalTask action(Long taskId, String action, String comment, HttpServletRequest request) {
        requireApproverRole(request);
        Long userId = currentUser(request);
        ApprovalTask task = tasks.findById(taskId).orElseThrow(() -> new BusinessException("NOT_FOUND", "Approval task does not exist", HttpStatus.NOT_FOUND));
        boolean assignedToMe = Objects.equals(task.assignedUserId, userId);
        boolean teacherQueue = roleGuard.hasRole(request, Roles.TEACHER)
                && Roles.TEACHER.equals(task.approverRole)
                && task.assignedUserId == null;
        boolean labAdminQueue = roleGuard.hasRole(request, Roles.LAB_ADMIN)
                && Roles.LAB_ADMIN.equals(task.approverRole)
                && task.assignedUserId == null;
        if (userId == null || (!assignedToMe && !teacherQueue && !labAdminQueue)) throw new BusinessException("FORBIDDEN", "Approval task is not assigned to the current user", HttpStatus.FORBIDDEN);
        if (teacherQueue || labAdminQueue) task.assignedUserId = userId;
        if (Objects.equals(task.applicantUserId, userId)) throw new BusinessException("SELF_APPROVAL_FORBIDDEN", "Approver cannot approve own booking", HttpStatus.FORBIDDEN);
        if (!"PENDING".equals(task.status)) throw new BusinessException("INVALID_STATUS", "Approval task has already been processed", HttpStatus.UNPROCESSABLE_ENTITY);
        if (expired(task)) {
            closeStaleTask(task.bookingId, "EXPIRED");
            throw new BusinessException("APPROVAL_EXPIRED", "Approval task has expired", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (!List.of("approve", "reject").contains(action)) throw new BusinessException("INVALID_ACTION", "Approval action is invalid", HttpStatus.BAD_REQUEST);
        String normalizedComment = comment == null ? null : comment.trim();
        if ("reject".equals(action) && (normalizedComment == null || normalizedComment.isBlank())) {
            throw new BusinessException("REJECTION_REASON_REQUIRED", "驳回时必须填写原因", HttpStatus.BAD_REQUEST);
        }
        if (normalizedComment != null && normalizedComment.length() > 500) {
            throw new BusinessException("INVALID_ARGUMENT", "审批说明不能超过 500 个字符", HttpStatus.BAD_REQUEST);
        }
        String requestId = Objects.toString(request.getAttribute("X-Request-Id"), "");
        if (!requestId.isBlank() && records.findByRequestIdAndTaskId(requestId, task.id).isPresent()) return task;
        String decision = "approve".equals(action) ? "APPROVED" : "REJECTED";
        try {
            BookingDecisionClient.BookingSnapshot booking = bookingDecisions.submit(
                    task.bookingId, decision, normalizedComment, task.level, Math.max(1, task.totalLevels),
                    request.getHeader("Authorization"));
            if ("APPROVED".equals(decision) && booking != null && "PENDING_APPROVAL".equals(booking.status())) {
                internalApprovals.createNext(task);
            }
        } catch (BusinessException exception) {
            if (STALE_BOOKING_CODES.contains(exception.code())) {
                closeStaleTask(task.bookingId, "BOOKING_EXPIRED".equals(exception.code()) ? "EXPIRED" : "CANCELED");
                throw new BusinessException("BOOKING_NOT_PENDING", "Booking is no longer pending approval", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            throw exception;
        }
        task.status = decision;
        task.comment = normalizedComment;
        task.completedAt = LocalDateTime.now();
        ApprovalTask saved = tasks.save(task);
        ApprovalRecord record = new ApprovalRecord();
        record.taskId = saved.id;
        record.bookingId = saved.bookingId;
        record.approverId = userId;
        record.result = saved.status;
        record.comment = normalizedComment;
        record.requestId = requestId.isBlank() ? null : requestId;
        records.save(record);
        return saved;
    }

    private void closeStaleTask(Long bookingId, String reason) {
        internalApprovals.closePending(bookingId, reason, null);
    }

    private Long currentUser(HttpServletRequest request) {
        if (request.getAttribute("userId") instanceof Long value) return value;
        throw new BusinessException("UNAUTHORIZED", "Login required", HttpStatus.UNAUTHORIZED);
    }

    private void requireApproverRole(HttpServletRequest request) {
        roleGuard.requireAny(request, Roles.TEACHER, Roles.LAB_ADMIN);
    }

    private boolean expired(ApprovalTask task) {
        LocalDateTime effective = task.deadline;
        if (task.startTime != null && (effective == null || task.startTime.isBefore(effective))) effective = task.startTime;
        return effective != null && !effective.isAfter(LocalDateTime.now());
    }
}
