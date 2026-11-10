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

import com.lab.booking.approval.service.ApprovalInternalService;
import com.lab.common.api.ApiResponse;
import com.lab.common.api.InternalServiceGuard;
import com.lab.common.api.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/internal/approvals")
public class InternalApprovalController {
    private final ApprovalInternalService approvals;
    private final InternalServiceGuard internalServices;

    public InternalApprovalController(ApprovalInternalService approvals, InternalServiceGuard internalServices) {
        this.approvals = approvals;
        this.internalServices = internalServices;
    }

    @PostMapping("/release/{userId}")
    public ApiResponse<?> release(@PathVariable("userId") Long userId, HttpServletRequest request) {
        internalServices.require(request);
        return ApiResponse.success(Map.of("released", approvals.releaseAssignedTo(userId)),
                Objects.toString(request.getAttribute(RequestIdFilter.HEADER), ""));
    }
}
