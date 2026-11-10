package com.lab.booking.service;

import com.lab.booking.entity.Booking;

public interface BookingInternalService {
    record ApprovalDecision(String status, String comment, Integer level, Integer totalLevels) {}

    Booking apply(Long bookingId, ApprovalDecision decision, Long operatorId, String requestId);
}
