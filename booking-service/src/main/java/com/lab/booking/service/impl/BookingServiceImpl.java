package com.lab.booking.service.impl;

import com.lab.booking.*;
import com.lab.booking.controller.BookingController;
import com.lab.booking.service.BookingService;
import com.lab.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookings;
    private final BookingSlotRepository slots;
    private final ResourceRuleClient resourceRules;
    private final ApprovalTaskClient approvalTasks;
    private final BookingLifecycleService lifecycle;

    public BookingServiceImpl(BookingRepository bookings, BookingSlotRepository slots, ResourceRuleClient resourceRules, ApprovalTaskClient approvalTasks, BookingLifecycleService lifecycle) {
        this.bookings = bookings;
        this.slots = slots;
        this.resourceRules = resourceRules;
        this.approvalTasks = approvalTasks;
        this.lifecycle = lifecycle;
    }

    @Override
    @Transactional
    public Booking create(BookingController.Create request, String key, HttpServletRequest servletRequest) {
        Long userId = currentUser(servletRequest);
        if (key == null || key.isBlank() || key.length() > 64) {
            throw new BusinessException("IDEMPOTENCY_REQUIRED", "Idempotency-Key is required", HttpStatus.BAD_REQUEST);
        }
        Optional<Booking> existing = bookings.findByUserIdAndClientRequestId(userId, key);
        if (existing.isPresent()) return existing.get();
        lifecycle.assertCanCreate(userId);
        if (!request.startTime().isBefore(request.endTime())) {
            throw new BusinessException("INVALID_TIME", "Start time must be before end time", HttpStatus.BAD_REQUEST);
        }
        ResourceRuleClient.BookingRule rule = resourceRules.getRule(request.resourceId(), request.startTime(), request.endTime(), request.participants(), servletRequest.getHeader("Authorization"));
        Booking booking = new Booking();
        booking.bookingNo = "BK" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        booking.userId = userId;
        booking.resourceId = request.resourceId();
        booking.resourceNameSnapshot = rule.resourceName();
        booking.applicantNameSnapshot = "User" + userId;
        booking.startTime = request.startTime();
        booking.endTime = request.endTime();
        booking.purpose = request.purpose();
        booking.participants = request.participants();
        booking.slotMinutesSnapshot = rule.slotMinutes();
        booking.approvalLevelSnapshot = rule.approvalLevel();
        booking.needCheckinSnapshot = rule.needCheckin();
        booking.clientRequestId = key;
        booking.status = rule.approvalLevel() == 0 ? "APPROVED" : "PENDING_APPROVAL";
        booking = bookings.saveAndFlush(booking);
        lifecycle.recordInitial(booking, userId, requestId(servletRequest));
        try {
            for (LocalDateTime slotTime = request.startTime(); slotTime.isBefore(request.endTime()); slotTime = slotTime.plusMinutes(rule.slotMinutes())) {
                BookingSlot slot = new BookingSlot();
                slot.resourceId = request.resourceId();
                slot.bookingId = booking.id;
                slot.slotStart = slotTime;
                slots.saveAndFlush(slot);
            }
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException("BOOKING_CONFLICT", "The selected time is already occupied", HttpStatus.CONFLICT);
        }
        if (rule.approvalLevel() > 0) {
            approvalTasks.create(booking.id, userId, rule.approvalLevel(), rule.approverUserId(), servletRequest.getHeader("Authorization"));
        }
        return booking;
    }

    @Override
    public List<Booking> my(HttpServletRequest servletRequest) { return bookings.findByUserIdOrderByStartTimeDesc(currentUser(servletRequest)); }

    @Override
    public List<LocalDateTime> occupied(Long resourceId, LocalDateTime start, LocalDateTime end, HttpServletRequest servletRequest) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new BusinessException("INVALID_TIME", "Start time must be before end time", HttpStatus.BAD_REQUEST);
        }
        return slots.findByResourceIdAndSlotStartBetweenAndReleasedAtIsNull(resourceId, start, end).stream()
                .map(slot -> slot.slotStart)
                .sorted()
                .toList();
    }

    @Override
    public Booking get(Long bookingId, HttpServletRequest servletRequest) {
        Booking booking = find(bookingId);
        if (!Objects.equals(booking.userId, currentUser(servletRequest))) {
            throw new BusinessException("FORBIDDEN", "You cannot view this booking", HttpStatus.FORBIDDEN);
        }
        return booking;
    }

    @Override
    @Transactional
    public Booking cancel(Long bookingId, HttpServletRequest servletRequest) {
        Long userId = currentUser(servletRequest);
        Booking booking = find(bookingId);
        if (!Objects.equals(booking.userId, userId)) throw new BusinessException("FORBIDDEN", "You cannot cancel this booking", HttpStatus.FORBIDDEN);
        if (!List.of("PENDING_APPROVAL", "APPROVED").contains(booking.status) || !booking.startTime.isAfter(LocalDateTime.now())) {
            throw new BusinessException("INVALID_STATUS", "Booking cannot be canceled", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        lifecycle.transition(booking, "CANCELED", userId, "Canceled by applicant", requestId(servletRequest));
        booking.canceledAt = LocalDateTime.now();
        lifecycle.releaseSlots(booking.id, "CANCELED");
        return bookings.save(booking);
    }

    @Override
    @Transactional
    public Booking checkin(Long bookingId, HttpServletRequest servletRequest) {
        Long userId = currentUser(servletRequest);
        Booking booking = find(bookingId);
        if (!Objects.equals(booking.userId, userId)) throw new BusinessException("FORBIDDEN", "You cannot check in for this booking", HttpStatus.FORBIDDEN);
        LocalDateTime now = LocalDateTime.now();
        if (!"APPROVED".equals(booking.status) || now.isBefore(booking.startTime.minusMinutes(15)) || now.isAfter(booking.startTime.plusMinutes(30))) {
            throw new BusinessException("CHECKIN_WINDOW", "Check-in is not currently available", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        lifecycle.transition(booking, "CHECKED_IN", userId, null, requestId(servletRequest));
        booking.checkinAt = now;
        return bookings.save(booking);
    }

    @Override
    @Transactional
    public void autoComplete() {
        LocalDateTime now = LocalDateTime.now();
        bookings.findAll().stream().filter(booking -> "APPROVED".equals(booking.status) && !booking.needCheckinSnapshot && booking.endTime != null && !booking.endTime.isAfter(now)).forEach(booking -> {
            lifecycle.transition(booking, "COMPLETED", null, "Automatically completed", null);
            booking.completedAt = now;
            bookings.save(booking);
            lifecycle.releaseSlots(booking.id, "COMPLETED");
        });
    }

    @Override
    @Transactional
    public void markNoShow() {
        LocalDateTime now = LocalDateTime.now();
        bookings.findAll().stream().filter(booking -> "APPROVED".equals(booking.status) && booking.needCheckinSnapshot && booking.startTime != null && now.isAfter(booking.startTime.plusMinutes(30))).forEach(booking -> {
            lifecycle.transition(booking, "NO_SHOW", null, "Check-in deadline passed", null);
            bookings.save(booking);
            lifecycle.releaseSlots(booking.id, "NO_SHOW");
            lifecycle.recordNoShow(booking);
        });
    }

    private Booking find(Long bookingId) { return bookings.findById(bookingId).orElseThrow(() -> new BusinessException("NOT_FOUND", "Booking does not exist", HttpStatus.NOT_FOUND)); }
    private Long currentUser(HttpServletRequest request) { return request.getAttribute("userId") instanceof Long userId ? userId : 1L; }
    private String requestId(HttpServletRequest request) { return Objects.toString(request.getAttribute("X-Request-Id"), ""); }
}
