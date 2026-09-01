package com.lab.booking;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;
import java.util.*;
public interface BookingRepository extends CrudMapper<Booking>{
    default Optional<Booking> findByUserIdAndClientRequestId(Long userId,String key) {
        return Optional.ofNullable(selectOne(Wrappers.<Booking>query().eq("user_id", userId).eq("client_request_id", key)));
    }
    default List<Booking> findByUserIdOrderByStartTimeDesc(Long userId) {
        return selectList(Wrappers.<Booking>query().eq("user_id", userId).orderByDesc("start_time"));
    }
    default List<Booking> findByStatus(String status) {
        return selectList(Wrappers.<Booking>query().eq("status", status));
    }

    default List<Booking> findOverlapping(Long resourceId, java.time.LocalDateTime start, java.time.LocalDateTime end) {
        return selectList(Wrappers.<Booking>query()
                .eq("resource_id", resourceId)
                .in("status", List.of("PENDING_APPROVAL", "APPROVED"))
                .lt("start_time", end)
                .gt("end_time", start));
    }

    default List<Booking> findActiveOccupancy(Long resourceId, java.time.LocalDateTime start, java.time.LocalDateTime end) {
        return selectList(Wrappers.<Booking>query()
                .eq("resource_id", resourceId)
                .in("status", List.of("PENDING_APPROVAL", "APPROVED", "CHECKED_IN"))
                .lt("start_time", end)
                .gt("end_time", start));
    }

    default List<Booking> findClosedByMaintenance(Long resourceId, java.time.LocalDateTime start, java.time.LocalDateTime end) {
        return selectList(Wrappers.<Booking>query()
                .eq("resource_id", resourceId)
                .eq("status", "CANCELED")
                .eq("cancel_reason", "RESOURCE_CLOSED")
                .lt("start_time", end)
                .gt("end_time", start));
    }
}
