package com.lab.approval.controller;

import com.lab.approval.service.ApprovalTaskService;
import com.lab.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/approvals")
public class ApprovalActionController {
    private final ApprovalTaskService service;
    public ApprovalActionController(ApprovalTaskService service) { this.service = service; }

    @GetMapping("/mine")
    public ApiResponse<?> mine(HttpServletRequest request) { return ApiResponse.success(service.mine(request), requestId(request)); }

    @PostMapping("/{id}/{action}")
    public ApiResponse<?> action(@PathVariable("id") Long id, @PathVariable("action") String action, @RequestBody(required = false) Map<String, String> body, HttpServletRequest request) {
        return ApiResponse.success(service.action(id, action, body == null ? null : body.get("comment"), request), requestId(request));
    }

    private String requestId(HttpServletRequest request) { return Objects.toString(request.getAttribute("X-Request-Id"), ""); }
}
