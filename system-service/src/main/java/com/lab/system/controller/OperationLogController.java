package com.lab.system.controller;

import com.lab.common.api.AdminOperationLogger;
import com.lab.common.api.ApiResponse;
import com.lab.system.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1")
public class OperationLogController {
    private final OperationLogService service;

    public OperationLogController(OperationLogService service) {
        this.service = service;
    }

    @GetMapping("/admin/operation-logs")
    public ApiResponse<?> list(@RequestParam(value = "operationType", required = false) String operationType,
                               @RequestParam(value = "result", required = false) String result,
                               @RequestParam(value = "keyword", required = false) String keyword,
                               HttpServletRequest request) {
        return ok(service.list(operationType, result, keyword, request), request);
    }

    @PostMapping("/internal/operation-logs")
    public ApiResponse<?> record(@RequestBody AdminOperationLogger.OperationLogRequest body,
                                 HttpServletRequest request) {
        return ok(service.record(body, request), request);
    }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest request) {
        return ApiResponse.success(data, Objects.toString(request.getAttribute("X-Request-Id"), ""));
    }
}
