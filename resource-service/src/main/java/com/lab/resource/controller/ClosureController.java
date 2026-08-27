package com.lab.resource.controller;

import com.lab.resource.ResourceClosure;
import com.lab.resource.service.ResourceClosureService;
import com.lab.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/admin/resources/{resourceId}/closures")
public class ClosureController {
    private final ResourceClosureService service;
    public ClosureController(ResourceClosureService service) { this.service = service; }
    @PostMapping public ApiResponse<?> create(@PathVariable("resourceId") Long resourceId, @RequestBody ResourceClosureRequest body, HttpServletRequest request) { return ok(service.create(resourceId, body, request), request); }
    @PostMapping("/{id}/cancel") public ApiResponse<?> cancel(@PathVariable("id") Long id, HttpServletRequest request) { return ok(service.cancel(id, request), request); }
    public record ResourceClosureRequest(String startTime, String endTime, String reason) {}
    private <T> ApiResponse<T> ok(T data, HttpServletRequest request) { return ApiResponse.success(data, Objects.toString(request.getAttribute("X-Request-Id"), "")); }
}
