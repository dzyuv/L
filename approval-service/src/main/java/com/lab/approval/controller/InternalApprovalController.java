package com.lab.approval.controller;

import com.lab.approval.ApprovalTask;
import com.lab.approval.service.ApprovalInternalService;
import com.lab.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;
import java.util.Objects;

/** Internal booking-service entry point. The gateway intentionally has no route for this path. */
@RestController
@RequestMapping("/api/v1/internal/approvals")
public class InternalApprovalController {
    private final ApprovalInternalService service;
    public InternalApprovalController(ApprovalInternalService service) { this.service = service; }

    public record CreateTask(@NotNull Long bookingId, @NotNull Long applicantUserId, String applicantName,
                             @NotNull Long resourceId, String resourceName,
                             @NotNull java.time.LocalDateTime startTime, @NotNull java.time.LocalDateTime endTime,
                             @Min(1) int level, Long assignedUserId, String approverRole) {}

    @PostMapping("/tasks")
    public ApiResponse<ApprovalTask> create(@Valid @RequestBody CreateTask request, HttpServletRequest servletRequest) {
        return ApiResponse.success(service.create(request, servletRequest), Objects.toString(servletRequest.getAttribute("X-Request-Id"), ""));
    }
}
