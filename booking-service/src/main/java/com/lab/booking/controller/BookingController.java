package com.lab.booking.controller;
import com.lab.booking.entity.*;
import com.lab.booking.repository.*;
import com.lab.booking.client.*;
import com.lab.booking.scheduler.*;
import com.lab.booking.config.*;
import com.lab.booking.service.*;
import com.lab.booking.service.impl.*;
import com.lab.booking.approval.entity.*;
import com.lab.booking.approval.repository.*;
import com.lab.booking.approval.service.*;
import com.lab.booking.approval.service.impl.*;
import com.lab.booking.approval.controller.*;

import com.lab.booking.service.BookingService;
import com.lab.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {
    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    public record Create(@NotNull Long resourceId, @NotNull LocalDateTime startTime, @NotNull LocalDateTime endTime, @NotBlank @Size(max = 500) String purpose, @Min(1) int participants) {}
    public record OccupiedInterval(LocalDateTime startTime, LocalDateTime endTime) {}

    @PostMapping
    public ApiResponse<?> create(@Valid @RequestBody Create request, @RequestHeader(value = "Idempotency-Key", required = false) String key, HttpServletRequest servletRequest) {
        return ApiResponse.success(service.create(request, key, servletRequest), requestId(servletRequest));
    }

    @GetMapping("/my")
    public ApiResponse<?> my(HttpServletRequest servletRequest) { return ApiResponse.success(service.my(servletRequest), requestId(servletRequest)); }

    @GetMapping("/resource/{resourceId}/occupied")
    public ApiResponse<?> occupied(@PathVariable("resourceId") Long resourceId,
                                   @RequestParam("start") LocalDateTime start,
                                   @RequestParam("end") LocalDateTime end,
                                   HttpServletRequest servletRequest) {
        return ApiResponse.success(service.occupied(resourceId, start, end, servletRequest), requestId(servletRequest));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> get(@PathVariable("id") Long id, HttpServletRequest servletRequest) { return ApiResponse.success(service.get(id, servletRequest), requestId(servletRequest)); }

    @PostMapping("/{id}/cancel")
    public ApiResponse<?> cancel(@PathVariable("id") Long id, HttpServletRequest servletRequest) { return ApiResponse.success(service.cancel(id, servletRequest), requestId(servletRequest)); }

    @PostMapping("/{id}/checkin")
    public ApiResponse<?> checkin(@PathVariable("id") Long id, HttpServletRequest servletRequest) { return ApiResponse.success(service.checkin(id, servletRequest), requestId(servletRequest)); }

    private String requestId(HttpServletRequest request) { return Objects.toString(request.getAttribute("X-Request-Id"), ""); }
}
