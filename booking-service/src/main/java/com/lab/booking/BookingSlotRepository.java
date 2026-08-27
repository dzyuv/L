package com.lab.booking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface BookingSlotRepository extends JpaRepository<BookingSlot,Long>{
    List<BookingSlot> findByBookingIdAndReleasedAtIsNull(Long bookingId);
    List<BookingSlot> findByResourceIdAndSlotStartGreaterThanEqualAndSlotStartLessThanAndReleasedAtIsNull(Long resourceId, java.time.LocalDateTime start, java.time.LocalDateTime end);
}
