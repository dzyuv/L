package com.lab.booking.service;

import com.lab.booking.Booking;
import com.lab.booking.controller.InternalBookingController;
import jakarta.servlet.http.HttpServletRequest;

public interface BookingInternalService {
    Booking apply(Long bookingId, InternalBookingController.ApprovalDecision decision, HttpServletRequest servletRequest);
}
