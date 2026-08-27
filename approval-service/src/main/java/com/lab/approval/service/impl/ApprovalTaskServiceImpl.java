package com.lab.approval.service.impl;

import com.lab.approval.*;
import com.lab.approval.service.ApprovalTaskService;
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

    public ApprovalTaskServiceImpl(ApprovalTaskRepository tasks, ApprovalRecordRepository records, BookingDecisionClient bookingDecisions) {
        this.tasks = tasks;
        this.records = records;
        this.bookingDecisions = bookingDecisions;
    }

    @Override
    public List<ApprovalTask> mine(HttpServletRequest request) {
        Long userId = request.getAttribute("userId") instanceof Long value ? value : 1L;
        return tasks.findByAssignedUserIdAndStatus(userId, "PENDING");
    }

    @Override
    @Transactional
    public ApprovalTask action(Long taskId, String action, String comment, HttpServletRequest request) {
        Long userId = request.getAttribute("userId") instanceof Long value ? value : null;
        ApprovalTask task = tasks.findById(taskId).orElseThrow(() -> new BusinessException("NOT_FOUND", "Approval task does not exist", HttpStatus.NOT_FOUND));
        if (userId == null || !Objects.equals(task.assignedUserId, userId)) throw new BusinessException("FORBIDDEN", "Approval task is not assigned to the current user", HttpStatus.FORBIDDEN);
        if (Objects.equals(task.applicantUserId, userId)) throw new BusinessException("SELF_APPROVAL_FORBIDDEN", "Approver cannot approve own booking", HttpStatus.FORBIDDEN);
        if (!"PENDING".equals(task.status)) throw new BusinessException("INVALID_STATUS", "Approval task has already been processed", HttpStatus.UNPROCESSABLE_ENTITY);
        if (!List.of("approve", "reject").contains(action)) throw new BusinessException("INVALID_ACTION", "Approval action is invalid", HttpStatus.BAD_REQUEST);
        String requestId = Objects.toString(request.getAttribute("X-Request-Id"), "");
        if (!requestId.isBlank() && records.findByRequestId(requestId).isPresent()) return task;
        task.status = "approve".equals(action) ? "APPROVED" : "REJECTED";
        task.comment = comment;
        task.completedAt = LocalDateTime.now();
        ApprovalTask saved = tasks.save(task);
        ApprovalRecord record = new ApprovalRecord();
        record.taskId = saved.id;
        record.bookingId = saved.bookingId;
        record.approverId = userId;
        record.result = saved.status;
        record.comment = comment;
        record.requestId = requestId.isBlank() ? null : requestId;
        records.save(record);
        bookingDecisions.submit(saved.bookingId, saved.status, request.getHeader("Authorization"));
        return saved;
    }
}
