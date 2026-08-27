package com.lab.booking.controller;

import com.lab.booking.service.BookingService;
import com.lab.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminBookingController {
    private final BookingService service;
    public AdminBookingController(BookingService service) { this.service = service; }
    public record ViolationRequest(@NotBlank String status, @Size(max = 500) String comment) {}

    @GetMapping("/bookings") public ApiResponse<?> bookings(@RequestParam(value="resourceId", required=false) Long resourceId,
            @RequestParam(value="userId", required=false) Long userId, @RequestParam(value="status", required=false) String status,
            HttpServletRequest request) { return ok(service.adminList(resourceId, userId, status, request), request); }
    @GetMapping("/violations") public ApiResponse<?> violations(HttpServletRequest request) { return ok(service.violations(request), request); }
    @PutMapping("/violations/{id}") public ApiResponse<?> process(@PathVariable("id") Long id, @Valid @RequestBody ViolationRequest body,
            HttpServletRequest request) { return ok(service.processViolation(id, body.status(), body.comment(), request), request); }
    private <T> ApiResponse<T> ok(T data, HttpServletRequest request) { return ApiResponse.success(data, Objects.toString(request.getAttribute("X-Request-Id"), "")); }
}
