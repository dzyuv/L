package com.lab.booking;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;
import java.util.List;
public interface BookingSlotRepository extends CrudMapper<BookingSlot>{
    default List<BookingSlot> findByBookingIdAndReleasedAtIsNull(Long bookingId) {
        return selectList(Wrappers.<BookingSlot>query().eq("booking_id", bookingId).isNull("released_at"));
    }
    default List<BookingSlot> findByResourceIdAndSlotStartGreaterThanEqualAndSlotStartLessThanAndReleasedAtIsNull(Long resourceId, java.time.LocalDateTime start, java.time.LocalDateTime end) {
        return selectList(Wrappers.<BookingSlot>query().eq("resource_id", resourceId)
                .ge("slot_start", start).lt("slot_start", end).isNull("released_at"));
    }
}
