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
import java.util.List;
public interface BookingSlotRepository extends CrudMapper<BookingSlot>{
    default List<BookingSlot> findByBookingId(Long bookingId) {
        return selectList(Wrappers.<BookingSlot>query().eq("booking_id", bookingId));
    }
    default List<BookingSlot> findByBookingIdAndReleasedAtIsNull(Long bookingId) {
        return selectList(Wrappers.<BookingSlot>query().eq("booking_id", bookingId).isNull("released_at"));
    }
}
