package com.lab.approval.service.impl;

import com.lab.approval.ApprovalTask;
import com.lab.approval.ApprovalTaskRepository;
import com.lab.approval.service.ApprovalQueryService;
import com.lab.common.api.RoleGuard;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import java.util.*;
import java.time.LocalDateTime;
import com.lab.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

@Service
public class ApprovalQueryServiceImpl implements ApprovalQueryService {
    private final ApprovalTaskRepository tasks;
    private final RoleGuard roleGuard;

    public ApprovalQueryServiceImpl(ApprovalTaskRepository tasks, RoleGuard roleGuard) {
        this.tasks = tasks;
        this.roleGuard = roleGuard;
    }

    @Override
    public Map<String, Object> pending(HttpServletRequest request) {
        roleGuard.requireAny(request, "TEACHER", "LAB_ADMIN");
        Long userId = request.getAttribute("userId") instanceof Long value ? value : null;
        if (userId == null) throw new BusinessException("UNAUTHORIZED", "Login required", HttpStatus.UNAUTHORIZED);
        List<ApprovalTask> items = tasks.findByAssignedUserIdAndStatus(userId, "PENDING").stream()
                .filter(task -> !Objects.equals(task.applicantUserId, userId))
                .filter(task -> !expired(task))
                .toList();
        return Map.of("items", items, "page", 1, "size", items.size(), "total", items.size());
    }

    private boolean expired(ApprovalTask task) {
        LocalDateTime effective = task.deadline;
        if (task.startTime != null && (effective == null || task.startTime.isBefore(effective))) effective = task.startTime;
        return effective != null && !effective.isAfter(LocalDateTime.now());
    }
}
