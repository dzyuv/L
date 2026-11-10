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

public interface BookingInternalService {
    record ApprovalDecision(String status, String comment, Integer level, Integer totalLevels) {}

    Booking apply(Long bookingId, ApprovalDecision decision, Long operatorId, String requestId);
}
