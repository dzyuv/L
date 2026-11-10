package com.lab.booking.repository;
import com.lab.booking.entity.*;
import com.lab.booking.client.*;
import com.lab.booking.scheduler.*;
import com.lab.booking.config.*;
import com.lab.booking.service.*;
import com.lab.booking.service.impl.*;
import com.lab.booking.controller.*;
import com.lab.booking.approval.entity.*;
import com.lab.booking.approval.repository.*;
import com.lab.booking.approval.service.*;
import com.lab.booking.approval.service.impl.*;
import com.lab.booking.approval.controller.*;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;

import java.util.Optional;

public interface BookingStatusHistoryRepository extends CrudMapper<BookingStatusHistory> {
    default Optional<BookingStatusHistory> findLatestCancel(Long bookingId) {
        return Optional.ofNullable(selectOne(Wrappers.<BookingStatusHistory>query()
                .eq("booking_id", bookingId)
                .eq("to_status", "CANCELED")
                .orderByDesc("created_at")
                .last("LIMIT 1")));
    }
}
