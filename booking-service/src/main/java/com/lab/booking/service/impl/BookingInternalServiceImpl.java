package com.lab.booking.service.impl;

import com.lab.booking.*;
import com.lab.booking.controller.InternalBookingController;
import com.lab.booking.service.BookingInternalService;
import com.lab.common.api.InternalServiceGuard;
import com.lab.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

@Service
public class BookingInternalServiceImpl implements BookingInternalService {
    private final BookingRepository bookings;
    private final BookingLifecycleService lifecycle;
    private final InternalServiceGuard internalServices;

    public BookingInternalServiceImpl(BookingRepository bookings, BookingLifecycleService lifecycle, InternalServiceGuard internalServices) {
        this.bookings = bookings;
        this.lifecycle = lifecycle;
        this.internalServices = internalServices;
    }

    @Override
    @Transactional
    public Booking apply(Long bookingId, InternalBookingController.ApprovalDecision decision, HttpServletRequest servletRequest) {
        internalServices.require(servletRequest);
        if (!"APPROVED".equals(decision.status()) && !"REJECTED".equals(decision.status())) {
            throw new BusinessException("INVALID_STATUS", "Approval decision is invalid", HttpStatus.BAD_REQUEST);
        }
        Booking booking = bookings.findById(bookingId).orElseThrow(() -> new BusinessException("NOT_FOUND", "Booking does not exist", HttpStatus.NOT_FOUND));
        if (!"PENDING_APPROVAL".equals(booking.status)) {
            throw new BusinessException("INVALID_STATUS", "Booking is not pending approval", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        Long operatorId = servletRequest.getAttribute("userId") instanceof Long value ? value : null;
        lifecycle.transition(booking, decision.status(), operatorId, "Approval decision", Objects.toString(servletRequest.getAttribute("X-Request-Id"), ""));
        if ("REJECTED".equals(decision.status())) lifecycle.releaseSlots(booking.id, "REJECTED");
        return bookings.save(booking);
    }
}
