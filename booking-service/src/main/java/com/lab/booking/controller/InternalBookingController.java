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
import com.lab.common.api.InternalServiceGuard;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Objects;

/** Internal resource-service callbacks. They are intentionally excluded from gateway routes. */
@RestController
@RequestMapping("/api/v1/internal/bookings")
public class InternalBookingController {
    private final BookingService bookings;
    private final InternalServiceGuard internalServices;

    public InternalBookingController(BookingService bookings, InternalServiceGuard internalServices) {
        this.bookings = bookings;
        this.internalServices = internalServices;
    }

    public record CloseForMaintenance(Long resourceId, LocalDateTime startTime, LocalDateTime endTime, String reason) {}

    @PostMapping("/close-for-maintenance")
    public ApiResponse<?> closeForMaintenance(@RequestBody CloseForMaintenance body, HttpServletRequest request) {
        internalServices.require(request);
        return ApiResponse.success(bookings.cancelOverlappingForClosure(body.resourceId(), body.startTime(), body.endTime(), body.reason(), request),
                Objects.toString(request.getAttribute("X-Request-Id"), ""));
    }

    @PostMapping("/restore-for-maintenance")
    public ApiResponse<?> restoreForMaintenance(@RequestBody CloseForMaintenance body, HttpServletRequest request) {
        internalServices.require(request);
        return ApiResponse.success(bookings.restoreOverlappingForClosure(body.resourceId(), body.startTime(), body.endTime(), request),
                Objects.toString(request.getAttribute("X-Request-Id"), ""));
    }
}
