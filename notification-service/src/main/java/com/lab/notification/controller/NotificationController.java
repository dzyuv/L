package com.lab.notification.controller;

import com.lab.common.api.ApiResponse;
import com.lab.notification.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.util.Objects;

@RestController @RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service = service; }
    @GetMapping("/my")
    public ApiResponse<?> my(HttpServletRequest request) { return ApiResponse.success(service.my(request), Objects.toString(request.getAttribute("X-Request-Id"), "")); }
    @PostMapping("/{id}/read")
    public ApiResponse<?> read(@PathVariable("id") Long id, HttpServletRequest request) { return ApiResponse.success(service.markRead(id, request), Objects.toString(request.getAttribute("X-Request-Id"), "")); }
}
