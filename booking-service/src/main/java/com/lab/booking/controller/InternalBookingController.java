package com.lab.booking.controller;

import com.lab.booking.Booking;
import com.lab.booking.service.BookingInternalService;
import com.lab.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;
import java.util.Objects;

/** Approval-service callback endpoint. It is intentionally excluded from gateway routes. */
@RestController
@RequestMapping("/api/v1/internal/bookings")
public class InternalBookingController {
    private final BookingInternalService service;
    public InternalBookingController(BookingInternalService service) { this.service = service; }
    public record ApprovalDecision(@NotBlank String status, @Size(max = 500) String comment) {}
    @PostMapping("/{id}/approval-decision")
    public ApiResponse<Booking> apply(@PathVariable("id") Long id, @Valid @RequestBody ApprovalDecision decision, HttpServletRequest request) {
        return ApiResponse.success(service.apply(id, decision, request), Objects.toString(request.getAttribute("X-Request-Id"), ""));
    }
}
