package com.lab.booking;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="violation_record",uniqueConstraints=@UniqueConstraint(columnNames={"bookingId","violationType"}))
public class ViolationRecord {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    public Long bookingId;
    public Long userId;
    public String violationType;
    public String status="OPEN";
    public String comment;
    public Long processedBy;
    public LocalDateTime processedAt;
    public LocalDateTime createdAt=LocalDateTime.now();
}
