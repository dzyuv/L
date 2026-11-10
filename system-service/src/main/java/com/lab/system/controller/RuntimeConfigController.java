package com.lab.system.controller;

import com.lab.common.api.ApiResponse;
import com.lab.common.api.InternalServiceGuard;
import com.lab.system.service.SystemConfigService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1")
public class RuntimeConfigController {
    private final SystemConfigService service;
    private final InternalServiceGuard internalServices;

    public RuntimeConfigController(SystemConfigService service, InternalServiceGuard internalServices) {
        this.service = service;
        this.internalServices = internalServices;
    }

    @GetMapping("/configs/runtime")
    public ApiResponse<?> runtime(HttpServletRequest request) {
        if (request.getAttribute("userId") == null) {
            return ApiResponse.error("UNAUTHORIZED", "Login required", Objects.toString(request.getAttribute("X-Request-Id"), ""));
        }
        return ApiResponse.success(service.runtime(), Objects.toString(request.getAttribute("X-Request-Id"), ""));
    }

    @GetMapping("/internal/configs")
    public ApiResponse<?> internal(HttpServletRequest request) {
        internalServices.require(request);
        return ApiResponse.success(service.allValues(), Objects.toString(request.getAttribute("X-Request-Id"), ""));
    }
}
