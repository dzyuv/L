package com.lab.booking.service;

import com.lab.booking.Booking;
import com.lab.booking.controller.BookingController;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Map;

public interface BookingService {
    Booking create(BookingController.Create request, String idempotencyKey, HttpServletRequest servletRequest);
    List<Booking> my(HttpServletRequest servletRequest);
    List<LocalDateTime> occupied(Long resourceId, LocalDateTime start, LocalDateTime end, HttpServletRequest servletRequest);
    Booking get(Long bookingId, HttpServletRequest servletRequest);
    Booking cancel(Long bookingId, HttpServletRequest servletRequest);
    Booking checkin(Long bookingId, HttpServletRequest servletRequest);
    void autoComplete();
    void markNoShow();
    void expirePendingApprovals();
    Map<String, Object> adminList(Long resourceId, Long userId, String status, HttpServletRequest servletRequest);
    Map<String, Object> violations(HttpServletRequest servletRequest);
    Object processViolation(Long violationId, String status, String comment, HttpServletRequest servletRequest);
    Map<String, Object> cancelOverlappingForClosure(Long resourceId, LocalDateTime start, LocalDateTime end, String reason, HttpServletRequest servletRequest);
}
