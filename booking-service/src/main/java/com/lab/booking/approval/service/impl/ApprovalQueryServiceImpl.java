package com.lab.booking.approval.service.impl;

import com.lab.booking.approval.entity.ApprovalTask;
import com.lab.booking.approval.repository.ApprovalTaskRepository;
import com.lab.booking.approval.service.ApprovalQueryService;
import com.lab.common.api.RoleGuard;
import com.lab.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
