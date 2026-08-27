package com.lab.booking;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface BookingRepository extends JpaRepository<Booking,Long>{
    Optional<Booking> findByUserIdAndClientRequestId(Long userId,String key);
    List<Booking> findByUserIdOrderByStartTimeDesc(Long userId);
}
