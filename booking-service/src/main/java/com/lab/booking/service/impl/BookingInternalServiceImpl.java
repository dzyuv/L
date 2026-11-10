package com.lab.booking.service.impl;
import com.lab.booking.entity.*;
import com.lab.booking.repository.*;
import com.lab.booking.client.*;
import com.lab.booking.scheduler.*;
import com.lab.booking.config.*;
import com.lab.booking.service.*;
import com.lab.booking.controller.*;
import com.lab.booking.approval.entity.*;
import com.lab.booking.approval.repository.*;
import com.lab.booking.approval.service.*;
import com.lab.booking.approval.service.impl.*;
import com.lab.booking.approval.controller.*;

import com.lab.booking.entity.Booking;
import com.lab.booking.service.BookingLifecycleService;
import com.lab.booking.repository.BookingRepository;
import com.lab.booking.service.BookingInternalService;
import com.lab.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingInternalServiceImpl implements BookingInternalService {
    private final BookingRepository bookings;
    private final BookingLifecycleService lifecycle;

    public BookingInternalServiceImpl(BookingRepository bookings, BookingLifecycleService lifecycle) {
        this.bookings = bookings;
        this.lifecycle = lifecycle;
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public Booking apply(Long bookingId, ApprovalDecision decision, Long operatorId, String requestId) {
        if (!"APPROVED".equals(decision.status()) && !"REJECTED".equals(decision.status())) {
            throw new BusinessException("INVALID_STATUS", "Approval decision is invalid", HttpStatus.BAD_REQUEST);
        }
        Booking booking = bookings.findById(bookingId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Booking does not exist", HttpStatus.NOT_FOUND));
        if (!"PENDING_APPROVAL".equals(booking.status)) {
            if (decision.status().equals(booking.status)) return booking;
            String code = switch (booking.status) {
                case "CANCELED" -> "BOOKING_CANCELED";
                case "EXPIRED" -> "BOOKING_EXPIRED";
                default -> "BOOKING_NOT_PENDING";
            };
            throw new BusinessException(code, "Booking is not pending approval", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        String comment = decision.comment() == null ? "" : decision.comment().trim();
        if ("REJECTED".equals(decision.status())) {
            if (comment.isBlank()) {
                throw new BusinessException("REJECTION_REASON_REQUIRED", "A rejection reason is required", HttpStatus.BAD_REQUEST);
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
            lifecycle.refreshApprovalDeadline(booking);
            return bookings.save(booking);
        }
        String historyReason = comment.isBlank() ? "Approval decision" : comment;
        lifecycle.transition(booking, "APPROVED", operatorId, historyReason, requestId);
        return bookings.save(booking);
    }
}
