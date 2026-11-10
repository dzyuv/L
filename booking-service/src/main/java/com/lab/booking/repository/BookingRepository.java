package com.lab.booking.repository;
import com.lab.booking.entity.Booking;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    default IPage<Booking> searchAdmin(Long resourceId, Long userId, String status, Page<Booking> page) {
        QueryWrapper<Booking> wrapper = new QueryWrapper<>();
        if (resourceId != null) wrapper.eq("resource_id", resourceId);
        if (userId != null) wrapper.eq("user_id", userId);
        if (status != null && !status.isBlank()) wrapper.eq("status", status);
        wrapper.orderByDesc("start_time");
        return selectPage(page, wrapper);
    }
}
