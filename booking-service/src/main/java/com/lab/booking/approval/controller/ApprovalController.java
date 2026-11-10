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

import com.lab.booking.approval.service.ApprovalQueryService;
import com.lab.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/approvals")
public class ApprovalController {
    private final ApprovalQueryService service;

    public ApprovalController(ApprovalQueryService service) {
        this.service = service;
    }

    @GetMapping("/pending")
    public ApiResponse<?> pending(HttpServletRequest request) {
        return ApiResponse.success(service.pending(request), Objects.toString(request.getAttribute("X-Request-Id"), ""));
    }
}
