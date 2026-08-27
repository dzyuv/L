package com.lab.booking.service;

import com.lab.booking.Booking;
import com.lab.booking.controller.BookingController;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.time.LocalDateTime;

public interface BookingService {
    Booking create(BookingController.Create request, String idempotencyKey, HttpServletRequest servletRequest);
    List<Booking> my(HttpServletRequest servletRequest);
    List<LocalDateTime> occupied(Long resourceId, LocalDateTime start, LocalDateTime end, HttpServletRequest servletRequest);
    Booking get(Long bookingId, HttpServletRequest servletRequest);
    Booking cancel(Long bookingId, HttpServletRequest servletRequest);
    Booking checkin(Long bookingId, HttpServletRequest servletRequest);
    void autoComplete();
    void markNoShow();
}
