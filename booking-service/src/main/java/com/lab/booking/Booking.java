package com.lab.booking;
import jakarta.persistence.*;
import java.time.*;
@Entity
@Table(name="booking", uniqueConstraints=@UniqueConstraint(name="uk_booking_user_request",columnNames={"user_id","client_request_id"}))
public class Booking{
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(unique=true,nullable=false) public String bookingNo;
    public Long userId;
    public Long resourceId;
    public String applicantNameSnapshot;
    public String resourceNameSnapshot;
    public LocalDateTime startTime;
    public LocalDateTime endTime;
    public int slotMinutesSnapshot;
    public String purpose;
    public int participants;
    public String status;
    public int approvalLevelSnapshot;
    public LocalDateTime approvalDeadline;
    public boolean needCheckinSnapshot;
    @Column(nullable=false) public String clientRequestId;
    public LocalDateTime checkinAt;
    public LocalDateTime completedAt;
    public LocalDateTime canceledAt;
    public String cancelReason;
    @Version public int version;
    public LocalDateTime createdAt=LocalDateTime.now();
    public LocalDateTime updatedAt=LocalDateTime.now();
}
