package com.lab.booking.repository;
import com.lab.booking.entity.BookingSlot;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;
import java.util.List;
public interface BookingSlotRepository extends CrudMapper<BookingSlot>{
    default List<BookingSlot> findByBookingId(Long bookingId) {
        return selectList(Wrappers.<BookingSlot>query().eq("booking_id", bookingId));
    }
    default List<BookingSlot> findByBookingIdAndReleasedAtIsNull(Long bookingId) {
        return selectList(Wrappers.<BookingSlot>query().eq("booking_id", bookingId).isNull("released_at"));
    }
}
