package com.lab.booking;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "booking_quota_lock")
public class BookingQuotaLock {
    @Id
    public Long userId;
}
