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
            if (decision.status().equals(booking.status)) return booking;
            String code = switch (booking.status) {
                case "CANCELED" -> "BOOKING_CANCELED";
                case "EXPIRED" -> "BOOKING_EXPIRED";
                default -> "BOOKING_NOT_PENDING";
            };
            throw new BusinessException(code, "Booking is not pending approval", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        Long operatorId = servletRequest.getAttribute("userId") instanceof Long value ? value : null;
        String comment = decision.comment() == null ? "" : decision.comment().trim();
        String requestId = Objects.toString(servletRequest.getAttribute("X-Request-Id"), "");
        if ("REJECTED".equals(decision.status())) {
            if (comment.isBlank()) {
                throw new BusinessException("REJECTION_REASON_REQUIRED", "驳回时必须填写原因", HttpStatus.BAD_REQUEST);
            }
            booking.rejectReason = comment;
            lifecycle.transition(booking, "REJECTED", operatorId, comment, requestId);
            lifecycle.releaseSlots(booking.id, "REJECTED");
            return bookings.save(booking);
        }
        int decidedLevel = decision.level() == null ? booking.approvalLevelSnapshot : decision.level();
        int totalLevels = decision.totalLevels() == null ? booking.approvalLevelSnapshot : decision.totalLevels();
        if (decidedLevel < Math.max(1, totalLevels)) {
            String historyReason = comment.isBlank() ? ("Level " + decidedLevel + " approved") : comment;
            lifecycle.recordProgress(booking, operatorId, historyReason, requestId);
            return bookings.save(booking);
        }
        String historyReason = comment.isBlank() ? "Approval decision" : comment;
        lifecycle.transition(booking, "APPROVED", operatorId, historyReason, requestId);
        return bookings.save(booking);
    }
}
