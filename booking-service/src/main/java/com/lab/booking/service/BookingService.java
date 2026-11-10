package com.lab.booking.service;
import com.lab.booking.entity.*;
import com.lab.booking.repository.*;
import com.lab.booking.client.*;
import com.lab.booking.scheduler.*;
import com.lab.booking.config.*;
import com.lab.booking.service.impl.*;
import com.lab.booking.controller.*;
import com.lab.booking.approval.entity.*;
import com.lab.booking.approval.repository.*;
import com.lab.booking.approval.service.*;
import com.lab.booking.approval.service.impl.*;
import com.lab.booking.approval.controller.*;

import com.lab.booking.entity.Booking;
import com.lab.booking.controller.BookingController;
import com.lab.common.api.PageQuery;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Map;

public interface BookingService {
    Booking create(BookingController.Create request, String idempotencyKey, HttpServletRequest servletRequest);
    List<Booking> my(HttpServletRequest servletRequest);
    List<BookingController.OccupiedInterval> occupied(Long resourceId, LocalDateTime start, LocalDateTime end, HttpServletRequest servletRequest);
    Booking get(Long bookingId, HttpServletRequest servletRequest);
    Booking cancel(Long bookingId, HttpServletRequest servletRequest);
    Booking checkin(Long bookingId, HttpServletRequest servletRequest);
    void autoComplete();
    void markNoShow();
    void expirePendingApprovals();
    Map<String, Object> adminList(Long resourceId, Long userId, String status, PageQuery pageQuery, HttpServletRequest servletRequest);
    Map<String, Object> violations(PageQuery pageQuery, HttpServletRequest servletRequest);
    Map<String, Object> violationStats(HttpServletRequest servletRequest);
    Object processViolation(Long violationId, String status, String comment, HttpServletRequest servletRequest);
    Map<String, Object> cancelOverlappingForClosure(Long resourceId, LocalDateTime start, LocalDateTime end, String reason, HttpServletRequest servletRequest);
    Map<String, Object> restoreOverlappingForClosure(Long resourceId, LocalDateTime start, LocalDateTime end, HttpServletRequest servletRequest);
}
