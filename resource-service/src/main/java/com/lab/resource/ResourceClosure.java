package com.lab.resource;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity public class ResourceClosure{
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    public Long resourceId;
    public LocalDateTime startTime;
    public LocalDateTime endTime;
    public String reason;
    public String status="PLANNED";
    public String handledBookingPolicy;
    public Long createdBy;
    public LocalDateTime updatedAt=LocalDateTime.now();
    @Version public int version;
    public LocalDateTime createdAt=LocalDateTime.now();
}
