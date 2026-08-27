package com.lab.booking;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="user_restriction")
public class UserRestriction {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    public Long userId;
    public LocalDateTime restrictedUntil;
    public String reason;
    public int sourceViolationCount;
    public String status="ACTIVE";
    public Long createdBy;
    public LocalDateTime createdAt=LocalDateTime.now();
    public LocalDateTime updatedAt=LocalDateTime.now();
}
