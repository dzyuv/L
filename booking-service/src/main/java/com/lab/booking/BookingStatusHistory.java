package com.lab.booking;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="booking_status_history")
public class BookingStatusHistory {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    public Long bookingId;
    public String fromStatus;
    public String toStatus;
    public Long operatorId;
    public String reason;
    public String requestId;
    public LocalDateTime createdAt=LocalDateTime.now();
}
