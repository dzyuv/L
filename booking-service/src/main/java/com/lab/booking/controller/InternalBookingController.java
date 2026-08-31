package com.lab.booking.controller;

import com.lab.booking.Booking;
import com.lab.booking.service.BookingInternalService;
import com.lab.booking.service.BookingService;
import com.lab.common.api.ApiResponse;
import com.lab.common.api.InternalServiceGuard;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Objects;

/** Approval-service callback endpoint. It is intentionally excluded from gateway routes. */
@RestController
@RequestMapping("/api/v1/internal/bookings")
public class InternalBookingController {
    private final BookingInternalService service;
    private final BookingService bookings;
    private final InternalServiceGuard internalServices;
    public InternalBookingController(BookingInternalService service, BookingService bookings, InternalServiceGuard internalServices) {
        this.service = service;
        this.bookings = bookings;
        this.internalServices = internalServices;
    }
    public record ApprovalDecision(@NotBlank String status, @Size(max = 500) String comment, Integer level, Integer totalLevels) {}
    @PostMapping("/{id}/approval-decision")
    public ApiResponse<Booking> apply(@PathVariable("id") Long id, @Valid @RequestBody ApprovalDecision decision, HttpServletRequest request) {
        return ApiResponse.success(service.apply(id, decision, request), Objects.toString(request.getAttribute("X-Request-Id"), ""));
    }

    @GetMapping("/statistics-source")
    public ApiResponse<?> statisticsSource(@RequestParam("start") LocalDateTime start, @RequestParam("end") LocalDateTime end, HttpServletRequest request) {
        internalServices.require(request);
        return ApiResponse.success(bookings.statisticsSource(start, end), Objects.toString(request.getAttribute("X-Request-Id"), ""));
    }
}
