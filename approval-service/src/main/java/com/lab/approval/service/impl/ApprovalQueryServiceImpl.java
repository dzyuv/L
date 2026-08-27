package com.lab.approval.service.impl;

import com.lab.approval.ApprovalTask;
import com.lab.approval.ApprovalTaskRepository;
import com.lab.approval.service.ApprovalQueryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import java.util.*;
import com.lab.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

@Service
public class ApprovalQueryServiceImpl implements ApprovalQueryService {
    private final ApprovalTaskRepository tasks;

    public ApprovalQueryServiceImpl(ApprovalTaskRepository tasks) { this.tasks = tasks; }

    @Override
    public Map<String, Object> pending(HttpServletRequest request) {
        Long userId = request.getAttribute("userId") instanceof Long value ? value : null;
        if (userId == null) throw new BusinessException("UNAUTHORIZED", "Login required", HttpStatus.UNAUTHORIZED);
        List<ApprovalTask> items = tasks.findByAssignedUserIdAndStatus(userId, "PENDING").stream()
                .filter(task -> !Objects.equals(task.applicantUserId, userId))
                .toList();
        return Map.of("items", items, "page", 1, "size", items.size(), "total", items.size());
    }
}
