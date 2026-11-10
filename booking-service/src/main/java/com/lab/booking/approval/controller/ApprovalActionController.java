package com.lab.booking.approval.controller;
import com.lab.booking.entity.*;
import com.lab.booking.repository.*;
import com.lab.booking.client.*;
import com.lab.booking.scheduler.*;
import com.lab.booking.config.*;
import com.lab.booking.service.*;
import com.lab.booking.service.impl.*;
import com.lab.booking.controller.*;
import com.lab.booking.approval.entity.*;
import com.lab.booking.approval.repository.*;
import com.lab.booking.approval.service.*;
import com.lab.booking.approval.service.impl.*;

import com.lab.booking.approval.service.ApprovalTaskService;
import com.lab.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/approvals")
public class ApprovalActionController {
    private final ApprovalTaskService service;

    public ApprovalActionController(ApprovalTaskService service) {
        this.service = service;
    }

    @GetMapping("/mine")
    public ApiResponse<?> mine(HttpServletRequest request) {
        return ApiResponse.success(service.mine(request), requestId(request));
    }

    @PostMapping("/{id}/{action}")
    public ApiResponse<?> action(@PathVariable("id") Long id, @PathVariable("action") String action,
                                 @RequestBody(required = false) Map<String, String> body,
                                 HttpServletRequest request) {
        return ApiResponse.success(service.action(id, action, body == null ? null : body.get("comment"), request), requestId(request));
    }

    private String requestId(HttpServletRequest request) {
        return Objects.toString(request.getAttribute("X-Request-Id"), "");
    }
}
