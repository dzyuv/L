package com.lab.booking;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity public class BookingSlot{
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    public Long resourceId;
    public Long bookingId;
    public LocalDateTime slotStart;
    public LocalDateTime releasedAt;
    public String releaseReason;
    public LocalDateTime createdAt=LocalDateTime.now();
}
