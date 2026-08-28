package com.lab.approval.service.impl;

import com.lab.approval.*;
import com.lab.approval.service.ApprovalTaskService;
import com.lab.common.api.RoleGuard;
import com.lab.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ApprovalTaskServiceImpl implements ApprovalTaskService {
    private final ApprovalTaskRepository tasks;
    private final ApprovalRecordRepository records;
    private final BookingDecisionClient bookingDecisions;
    private final RoleGuard roleGuard;

    public ApprovalTaskServiceImpl(ApprovalTaskRepository tasks, ApprovalRecordRepository records,
                                   BookingDecisionClient bookingDecisions, RoleGuard roleGuard) {
        this.tasks = tasks;
        this.records = records;
        this.bookingDecisions = bookingDecisions;
        this.roleGuard = roleGuard;
    }

    @Override
    public List<ApprovalTask> mine(HttpServletRequest request) {
        requireApproverRole(request);
        Long userId = currentUser(request);
        return tasks.findByAssignedUserIdAndStatus(userId, "PENDING").stream()
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
        if (userId == null || !Objects.equals(task.assignedUserId, userId)) throw new BusinessException("FORBIDDEN", "Approval task is not assigned to the current user", HttpStatus.FORBIDDEN);
        if (Objects.equals(task.applicantUserId, userId)) throw new BusinessException("SELF_APPROVAL_FORBIDDEN", "Approver cannot approve own booking", HttpStatus.FORBIDDEN);
        if (!"PENDING".equals(task.status)) throw new BusinessException("INVALID_STATUS", "Approval task has already been processed", HttpStatus.UNPROCESSABLE_ENTITY);
        if (expired(task)) throw new BusinessException("APPROVAL_EXPIRED", "Approval task has expired", HttpStatus.UNPROCESSABLE_ENTITY);
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
        task.status = "approve".equals(action) ? "APPROVED" : "REJECTED";
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
        bookingDecisions.submit(saved.bookingId, saved.status, request.getHeader("Authorization"));
        return saved;
    }

    private Long currentUser(HttpServletRequest request) {
        if (request.getAttribute("userId") instanceof Long value) return value;
        throw new BusinessException("UNAUTHORIZED", "Login required", HttpStatus.UNAUTHORIZED);
    }

    private void requireApproverRole(HttpServletRequest request) {
        roleGuard.requireAny(request, "TEACHER", "LAB_ADMIN");
    }

    private boolean expired(ApprovalTask task) {
        LocalDateTime effective = task.deadline;
        if (task.startTime != null && (effective == null || task.startTime.isBefore(effective))) effective = task.startTime;
        return effective != null && !effective.isAfter(LocalDateTime.now());
    }
}
