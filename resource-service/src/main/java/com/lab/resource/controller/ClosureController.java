package com.lab.resource.controller;

import com.lab.resource.ResourceClosure;
import com.lab.resource.service.ResourceClosureService;
import com.lab.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/admin/resources/{resourceId}/closures")
public class ClosureController {

    private final ResourceClosureService service;

    public ClosureController(ResourceClosureService service) {
        this.service = service;
    }
    @GetMapping public ApiResponse<?> list(@PathVariable("resourceId") Long resourceId, HttpServletRequest request) {
        return ok(service.list(resourceId, request), request);
    }
    @PostMapping public ApiResponse<?> create(@PathVariable("resourceId") Long resourceId, @Valid @RequestBody ResourceClosureRequest body, HttpServletRequest request) {
        return ok(service.create(resourceId, body, request), request);
    }
    @PostMapping("/{id}/cancel") public ApiResponse<?> cancel(@PathVariable("id") Long id, HttpServletRequest request) {
        return ok(service.cancel(id, request), request);
    }
    public record ResourceClosureRequest(@NotBlank String startTime, @NotBlank String endTime, @Size(max = 500) String reason) {}
    private <T> ApiResponse<T> ok(T data, HttpServletRequest request) {
        return ApiResponse.success(data, Objects.toString(request.getAttribute("X-Request-Id"), ""));
    }
}
