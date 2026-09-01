package com.lab.booking;

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
