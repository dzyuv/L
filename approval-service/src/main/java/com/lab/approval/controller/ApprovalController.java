package com.lab.approval.controller;

import com.lab.approval.service.ApprovalQueryService;
import com.lab.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/approvals")
public class ApprovalController {
    private final ApprovalQueryService service;

    public ApprovalController(ApprovalQueryService service) { this.service = service; }

    @GetMapping("/pending")
    public ApiResponse<?> pending(HttpServletRequest request) {
        return ApiResponse.success(service.pending(request), Objects.toString(request.getAttribute("X-Request-Id"), ""));
    }
}
